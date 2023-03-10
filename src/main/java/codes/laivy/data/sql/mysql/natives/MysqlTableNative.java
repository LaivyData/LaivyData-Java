package codes.laivy.data.sql.mysql.natives;

import codes.laivy.data.sql.SqlReceptor;
import codes.laivy.data.sql.SqlVariable;
import codes.laivy.data.sql.mysql.MysqlDatabase;
import codes.laivy.data.sql.mysql.MysqlTable;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 *     The native MySQL Table of LaivyData.
 *     This native table autoload at constructor.
 * </p>
 *
 * @author Laivy
 * @since 1.0
 *
 * @version 1.0 - (01/03/2023)
 */
public class MysqlTableNative implements MysqlTable {

    private final @NotNull MysqlDatabase database;
    private @NotNull String id;

    private final @NotNull Set<SqlReceptor> receptors = new LinkedHashSet<>();
    private final @NotNull Set<SqlVariable> variables = new LinkedHashSet<>();

    private boolean loaded = false;

    public MysqlTableNative(@NotNull MysqlDatabase database, @NotNull String id) {
        this.database = database;
        this.id = id;

        if (!database.isLoaded()) {
            throw new IllegalStateException("This database isn't loaded!");
        }

        load();
    }

    @Override
    public void load() {
        getDatabase().getManager().getTablesManager().load(this);
        getDatabase().getLoadedTables().add(this);
        loaded = true;
    }

    @Override
    public void unload() {
        getDatabase().getManager().getTablesManager().unload(this);
        getDatabase().getLoadedTables().remove(this);
        loaded = false;
    }

    @Override
    public void delete() {
        unload();
        getDatabase().getManager().getTablesManager().delete(this);
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public @NotNull Set<SqlReceptor> getLoadedReceptors() {
        return receptors;
    }

    @Override
    public @NotNull Set<SqlVariable> getLoadedVariables() {
        return variables;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public void setId(@NotNull String id) {
        this.id = id;
    }

    @Override
    public @NotNull MysqlDatabase getDatabase() {
        return database;
    }
}
