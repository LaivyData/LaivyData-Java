package codes.laivy.data.sqlite.utils;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Set;

public final class SqlUtils {

    @ApiStatus.Internal
    private SqlUtils() {
        throw new UnsupportedOperationException();
    }

    public static int getErrorCode(@NotNull Throwable throwable) {
        if (throwable instanceof SQLException) {
            return ((SQLException) throwable).getErrorCode();
        }
        return -1;
    }

    public static @NotNull String rowNotIn(@NotNull Set<Integer> excluded) {
        @NotNull StringBuilder builder = new StringBuilder();

        if (!excluded.isEmpty()) {
            builder.append("`row` NOT IN (");

            int index = 0;
            for (int row : excluded) {
                builder.append(row);

                if (index + 1 < excluded.size()) {
                    builder.append(",");
                }

                index++;
            }
            builder.append(")");
        } else {
            builder.append("`row` NOT IN (-999)");
        }

        return builder.toString();
    }

    @ApiStatus.Internal
    public static @NotNull String buildWhereCondition(@NotNull Set<Integer> excluded, @NotNull Condition<?> @NotNull ... conditions) {
        @NotNull StringBuilder builder = new StringBuilder("WHERE");

        int index = 0;
        for (@NotNull Condition<?> condition : conditions) {
            if (index > 0) builder.append(" && ");
            builder.append(" `").append(condition.getVariable().getId()).append("` = ?");
            index++;
        }

        if (!excluded.isEmpty()) {
            builder.append(" && ").append(rowNotIn(excluded));
        }

        return builder.toString();
    }

}
