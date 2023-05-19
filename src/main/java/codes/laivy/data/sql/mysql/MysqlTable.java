package codes.laivy.data.sql.mysql;

import codes.laivy.data.sql.SqlTable;
import codes.laivy.data.sql.SqlVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * @author Laivy
 * @since 1.0
 */
public interface MysqlTable extends SqlTable {
    @Override
    @NotNull MysqlDatabase getDatabase();

    @Override
    default @Nullable MysqlVariable getLoadedVariable(@NotNull String id) {
        for (SqlVariable table : getLoadedVariables()) {
            if (table.getId().equals(id)) {
                return (MysqlVariable) table;
            }
        }
        return null;
    }

    @Range(from = 0, to = Long.MAX_VALUE) long getAutoIncrement();
}
