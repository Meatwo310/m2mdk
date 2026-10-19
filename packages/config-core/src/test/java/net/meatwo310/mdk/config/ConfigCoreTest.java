package net.meatwo310.mdk.config;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ConfigCoreTest {
    private enum Mode { FIRST, SECOND }

    @Test
    public void unboundEntriesExposeDefaultsButRejectWrites() {
        var builder = new ConfigEntryBuilder();
        var number = builder.define("number", 12);
        var enabled = builder.define("enabled", true);
        assertEquals(12, number.getAsInt());
        assertTrue(enabled.getAsBoolean());
        assertThrows(IllegalStateException.class, () -> number.set(13));
        assertThrows(IllegalStateException.class, number::flush);
    }

    @Test
    public void writesAndFlushesUseTheBoundBackend() {
        var builder = new ConfigEntryBuilder();
        var number = builder.define("number", 12);
        var adapter = bind(builder.build());
        number.set(13);
        assertEquals(13, number.getAsInt());
        assertEquals(13, adapter.values.get("number"));
        assertEquals(0, adapter.flushes);
        number.flush();
        number.setAndFlush(14);
        assertEquals(2, adapter.flushes);
        assertEquals(14, number.getAsInt());
        assertEquals(Integer.valueOf(12), number.defaultValue());
    }

    @Test
    public void supplierBindingsRemainReadOnlyAndCanBeReplaced() {
        var number = new ConfigEntryBuilder().define("number", 1);
        number.bind(() -> 5);
        assertEquals(5, number.getAsInt());
        assertThrows(UnsupportedOperationException.class, () -> number.set(6));
        assertThrows(UnsupportedOperationException.class, number::flush);
        var storage = new int[]{7};
        number.bind(ConfigEntryBinding.of(() -> storage[0], next -> storage[0] = next, () -> {}));
        number.setAndFlush(8);
        assertEquals(8, storage[0]);
    }

    @Test
    public void allEntryKindsForwardTheirMetadataToTheAdapter() {
        var builder = new ConfigEntryBuilder();
        var integer = builder.defineInRange("integer", 2, 1, 3);
        var longEntry = builder.defineInRange("long", 20L, 10L, 30L);
        var decimal = builder.defineInRange("decimal", 0.5, 0.0, 1.0);
        var bool = builder.define("bool", true);
        var text = builder.define("text", "default");
        var original = new ArrayList<>(List.of("one"));
        var list = builder.defineList("list", original, () -> "new", value -> value instanceof String);
        var mode = builder.defineEnum("mode", Mode.FIRST);
        original.add("two");
        var adapter = bind(builder.build());

        assertEquals(List.of("integer:1:3", "long:10:30", "decimal:0.0:1.0"), adapter.ranges);
        assertEquals(2, integer.getAsInt());
        assertEquals(20L, longEntry.getAsLong());
        assertEquals(0.5, decimal.getAsDouble(), 0.0);
        assertTrue(bool.getAsBoolean());
        assertEquals("default", text.get());
        assertEquals(List.of("one"), list.defaultValue());
        assertThrows(UnsupportedOperationException.class, () -> list.defaultValue().add("other"));
        assertEquals("new", adapter.newElement.get());
        assertTrue(adapter.validator.test("valid"));
        assertFalse(adapter.validator.test(1));
        assertEquals(Mode.class, mode.enumClass());
        mode.set(Mode.SECOND);
        assertEquals(Mode.SECOND, mode.get());
    }

    @Test
    public void nestedDeclarationsKeepInstructionOrderAndSkipBlankComments() {
        var nested = new ConfigEntryBuilder();
        nested.comment("   ").define("inside", true);
        var builder = new ConfigEntryBuilder();
        builder.comment("first").comment("second").translation("custom.category")
                .worldRestart().gameRestart().category("advanced", nested.build());
        builder.push("a").push("b").pop(2);
        builder.define("after", "value");
        var adapter = bind(builder.build());
        assertEquals(List.of(
                "translation:custom.category", "worldRestart", "gameRestart", "comment:first\nsecond",
                "push:advanced", "define:inside", "pop", "push:a", "push:b", "pop:2", "define:after"
        ), adapter.events);
    }

    @Test
    public void buildAndAppendDoNotMutateEarlierEntryCollections() {
        var builder = new ConfigEntryBuilder();
        builder.define("first", true);
        var first = builder.build();
        builder.define("second", false);
        assertEquals(Map.of("first", true), bind(first).values);
        assertEquals(Map.of("first", true, "second", false), bind(builder.build()).values);
        var extra = new ConfigEntryBuilder();
        extra.define("third", 3);
        var merged = first.append(extra.build());
        assertEquals(Map.of("first", true, "third", 3), bind(merged).values);
        assertEquals(Map.of("first", true), bind(first).values);
    }

    @Test
    public void declarationsNormalizeNamesAndAppendByFileIdentity() {
        var base = new ConfigEntryBuilder();
        base.define("base", true);
        var extra = new ConfigEntryBuilder();
        extra.define("extra", 2);
        var server = ConfigDeclaration.of(ConfigSide.SYNCED, base.build(), " server.toml ");
        var client = ConfigDeclaration.of(ConfigSide.CLIENT, base.build());
        assertEquals("server.toml", server.fileName());
        assertTrue(ConfigDeclaration.of(ConfigSide.LOCAL, base.build(), "  ").optionalFileName().isEmpty());
        var equivalentTarget = ConfigDeclaration.of(ConfigSide.SYNCED, extra.build(), "server.toml");
        var result = ConfigDeclarations.append(List.of(server, client), equivalentTarget, extra.build());
        assertEquals(Map.of("base", true, "extra", 2), bind(result.get(0).entries()).values);
        assertSame(client, result.get(1));
        assertEquals(Map.of("base", true), bind(server.entries()).values);
        assertThrows(UnsupportedOperationException.class, () -> result.add(server));
        assertThrows(IllegalArgumentException.class, () ->
                ConfigDeclarations.append(List.of(client), server, extra.build()));
        assertThrows(IllegalArgumentException.class, () ->
                ConfigDeclarations.append(List.of(server, server), server, extra.build()));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void sideAliasesPreserveTheirIdentityWithoutAddingEnumConstants() {
        assertSame(ConfigSide.LOCAL, ConfigSide.COMMON);
        assertSame(ConfigSide.SYNCED, ConfigSide.SERVER);
        assertArrayEquals(new ConfigSide[]{ConfigSide.LOCAL, ConfigSide.SYNCED, ConfigSide.CLIENT}, ConfigSide.values());
    }

    private static MemoryAdapter bind(ConfigEntries entries) {
        var adapter = new MemoryAdapter();
        entries.bindTo(new ConfigEntryBinder(adapter));
        return adapter;
    }

    /** In-memory backend exercises the public adapter contract without any loader classes. */
    private static final class MemoryAdapter implements ConfigEntryBinder.Adapter {
        private final Map<String, Object> values = new HashMap<>();
        private final List<String> events = new ArrayList<>();
        private final List<String> ranges = new ArrayList<>();
        private int flushes;
        private Supplier<?> newElement;
        private Predicate<Object> validator;

        @Override public void comment(String comment) { events.add("comment:" + comment); }
        @Override public void translation(String key) { events.add("translation:" + key); }
        @Override public void worldRestart() { events.add("worldRestart"); }
        @Override public void gameRestart() { events.add("gameRestart"); }
        @Override public void push(String key) { events.add("push:" + key); }
        @Override public void pop() { events.add("pop"); }
        @Override public void pop(int count) { events.add("pop:" + count); }

        @Override
        public ConfigEntryBinding<Integer> defineIntInRange(String key, int value, int min, int max) {
            ranges.add(key + ":" + min + ":" + max);
            return value(key, value);
        }

        @Override
        public ConfigEntryBinding<Long> defineLongInRange(String key, long value, long min, long max) {
            ranges.add(key + ":" + min + ":" + max);
            return value(key, value);
        }

        @Override
        public ConfigEntryBinding<Double> defineDoubleInRange(String key, double value, double min, double max) {
            ranges.add(key + ":" + min + ":" + max);
            return value(key, value);
        }

        @Override public ConfigEntryBinding<Boolean> defineBoolean(String key, boolean value) { return value(key, value); }
        @Override public ConfigEntryBinding<String> defineString(String key, String value) { return value(key, value); }

        @Override
        public <T> ConfigEntryBinding<List<T>> defineList(String key, List<T> defaults, Supplier<T> supplier, Predicate<Object> predicate) {
            newElement = supplier;
            validator = predicate;
            return value(key, defaults);
        }

        @Override
        public <E extends Enum<E>> ConfigEntryBinding<E> defineEnum(String key, E defaults) { return value(key, defaults); }

        @SuppressWarnings("unchecked")
        private <T> ConfigEntryBinding<T> value(String key, T defaults) {
            events.add("define:" + key);
            values.put(key, defaults);
            return ConfigEntryBinding.of(() -> (T) values.get(key), next -> values.put(key, next), () -> flushes++);
        }
    }
}
