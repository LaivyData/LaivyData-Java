package codes.laivy.data.api.database;

import codes.laivy.data.api.manager.DatabaseManager;
import org.jetbrains.annotations.NotNull;

/**
 * The database will be responsible for storing all the variables and their dependencies between database manager.
 * Normally, the database will not have the connection object or reference (like the MySQL driver); the database manager will.
 *
 * @author Laivy
 * @since 1.0
 */
public interface Database {

    /**
     * Some databases requires a special regex for id.
     * @return the id of that database
     *
     * @author Laivy
     * @since 1.0
     */
    @NotNull String getId();

    @NotNull DatabaseManager<?, ?, ?, ?> getManager();

    void load();

    void unload();

    void delete();

    boolean isLoaded();

}
