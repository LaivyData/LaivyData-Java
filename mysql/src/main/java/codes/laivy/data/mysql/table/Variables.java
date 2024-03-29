package codes.laivy.data.mysql.table;

import codes.laivy.data.content.Content;
import codes.laivy.data.mysql.variable.MysqlVariable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class Variables extends Content.SetProvider<MysqlVariable<?>> {

    private final @NotNull Default defaultVariables = new Default();
    private final @NotNull MysqlTable table;

    public Variables(@NotNull MysqlTable table) {
        super(new HashSet<>());
        this.table = table;
    }

    public @NotNull Default getDefault() {
        return defaultVariables;
    }

    @Contract(pure = true)
    public @NotNull MysqlTable getTable() {
        return table;
    }

    public @NotNull Optional<MysqlVariable<?>> getById(@NotNull String id) {
        return stream().filter(v -> v.getId().equalsIgnoreCase(id)).findFirst();
    }

    @Override
    public boolean add(@NotNull MysqlVariable<?> object) {
        if (!getTable().isLoaded()) {
            throw new IllegalStateException("The table aren't loaded");
        } else if (stream().anyMatch(var -> var.getId().equalsIgnoreCase(object.getId()))) {
            throw new IllegalStateException("A variable with id '" + object.getId() + "' already are added at table '" + getTable().getId() + "'");
        } else if (!object.getTable().equals(getTable())) {
            throw new IllegalStateException("Illegal variable table '" + object.getId() + "'");
        }

        synchronized (this) {
            return super.add(object);
        }
    }

    @Override
    public boolean remove(@NotNull MysqlVariable<?> object) {
        if (!getTable().isLoaded()) {
            throw new IllegalStateException("The variable aren't loaded");
        } else if (!object.getTable().equals(getTable())) {
            throw new IllegalStateException("Illegal variable table '" + object.getId() + "'");
        }

        synchronized (this) {
            return super.remove(object);
        }
    }

    public boolean contains(@NotNull String id) {
        return stream().anyMatch(variable -> variable.getId().equalsIgnoreCase(id));
    }

    public @NotNull Optional<MysqlVariable<?>> get(@NotNull String id) {
        return stream().filter(variable -> variable.getId().equalsIgnoreCase(id)).findFirst();
    }

    @Override
    public @NotNull Iterator<MysqlVariable<?>> iterator() {
        synchronized (this) {
            return super.iterator();
        }
    }

    @Override
    public @Unmodifiable @NotNull Collection<MysqlVariable<?>> toCollection() {
        synchronized (this) {
            return super.toCollection();
        }
    }

    // Classes

    public final class Default implements Iterable<MysqlVariable<?>> {

        private final @NotNull Set<MysqlVariable<?>> variables = new HashSet<>();

        public void addAll(@NotNull MysqlVariable<?>... variables) {
            for (MysqlVariable<?> variable : variables) {
                if (!variable.getTable().equals(getTable())) {
                    throw new IllegalStateException("Illegal default variable table");
                }
                this.variables.add(variable);
            }
        }
        public boolean add(@NotNull MysqlVariable<?> object) {
            if (!object.getTable().equals(getTable())) {
                throw new IllegalStateException("Illegal default variable table");
            }
            return variables.add(object);
        }
        public boolean remove(@NotNull MysqlVariable<?> object) {
            return variables.remove(object);
        }

        @NotNull
        @Override
        public Iterator<MysqlVariable<?>> iterator() {
            return variables.iterator();
        }
    }

}
