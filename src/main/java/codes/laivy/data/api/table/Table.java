package codes.laivy.data.api.table;

import codes.laivy.data.api.database.Database;
import org.jetbrains.annotations.NotNull;

/**
 * A table is a good way to order your database and separate the receptors and variable by category
 *
 * @author Laivy
 * @since 1.0
 */
public interface Table {

    /**
     * Gets the table's receptor
     * @return the database of this table
     *
     * @author Laivy
     * @since 1.0
     */
    @NotNull Database getDatabase();

    /**
     * The id will be used to identify this table at the database, some databases uses custom regexes.
     * @return the receptor id
     *
     * @author Laivy
     * @since 1.0
     */
    @NotNull String getId();

    /**
     * This will change the table's database id.
     * @param id the new table id
     *
     * @author Laivy
     * @since 1.0
     */
    void setId(@NotNull String id);

    void load();

    void unload();

    void delete();

    boolean isLoaded();

}
