package codes.laivy.data.mysql.table;

import codes.laivy.data.mysql.data.MysqlData;
import codes.laivy.data.mysql.database.MysqlDatabase;
import codes.laivy.data.mysql.utils.SqlUtils;
import codes.laivy.data.mysql.variable.MysqlVariable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MysqlTable {

    private final @NotNull String name;
    private final @NotNull MysqlDatabase database;
    private final @NotNull Variables variables;

    @ApiStatus.Internal
    private boolean loaded = false;

    public MysqlTable(@NotNull String name, @NotNull MysqlDatabase database) {
        this.name = name;
        this.database = database;

        this.variables = new Variables(this);

        if (!name.matches("^[a-zA-Z0-9_]{0,63}$")) {
            throw new IllegalStateException("This table name '" + name + "' doesn't follows the regex '^[a-zA-Z0-9_]{0,63}$'");
        }
    }

    public @NotNull CompletableFuture<Void> start() {
        if (isLoaded()) {
            throw new IllegalStateException("The mysql table '" + getName() + "' is already loaded");
        }

        @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                load().join();

                getDatabase().getTables().add(this);

                loaded = true;
                future.complete(null);
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }
    public @NotNull CompletableFuture<Void> stop() {
        if (!isLoaded()) {
            throw new IllegalStateException("The mysql table '" + getName() + "' is not loaded");
        }

        @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                unload().join();

                for (MysqlVariable<?> variable : getVariables()) {
                    if (variable.isLoaded()) {
                        variable.stop().join();
                    }
                }
                for (@NotNull MysqlData data : getReceptors()) {
                    if (data.isLoaded()) {
                        data.stop(true).join();
                    }
                }

                MysqlData.CACHED_LOADED_DATAS.remove(this);
                getVariables().clear();

                getDatabase().getTables().remove(this);

                loaded = false;
                future.complete(null);
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }

    private @NotNull CompletableFuture<Void> load() {
        @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                create().join();

                future.complete(null);
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }
    private @NotNull CompletableFuture<Void> unload() {
        return CompletableFuture.completedFuture(null);
    }

    public @NotNull CompletableFuture<Boolean> create() {
        @Nullable Connection connection = getDatabase().getAuthentication().getConnection();
        if (connection == null) {
            throw new IllegalStateException("The database's authentication aren't connected");
        }

        @NotNull CompletableFuture<Boolean> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = getDatabase().getAuthentication().getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS `" + getDatabase().getId() + "`.`" + getName() + "` (`row` INT AUTO_INCREMENT PRIMARY KEY);")) {
                if (isLoaded()) {
                    unload().join();
                }

                statement.execute();

                future.complete(true);
            } catch (@NotNull Throwable throwable) {
                if (SqlUtils.getErrorCode(throwable) == 1051) {
                    future.complete(false);
                } else {
                    future.completeExceptionally(throwable);
                }
            }
        });

        return future;
    }
    public @NotNull CompletableFuture<Boolean> delete() {
        @Nullable Connection connection = getDatabase().getAuthentication().getConnection();
        if (connection == null) {
            throw new IllegalStateException("The database's authentication aren't connected");
        }

        @NotNull CompletableFuture<Boolean> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = getDatabase().getAuthentication().getConnection().prepareStatement("DROP TABLE `" + getDatabase().getId() + "`.`" + getName() + "`")) {
                if (isLoaded()) {
                    stop().join();
                }

                statement.execute();

                future.complete(true);
            } catch (@NotNull Throwable throwable) {
                if (SqlUtils.getErrorCode(throwable) == 1051) {
                    future.complete(false);
                } else {
                    future.completeExceptionally(throwable);
                }
            }
        });

        return future;
    }
    public @NotNull CompletableFuture<Boolean> exists() {
        @Nullable Connection connection = getDatabase().getAuthentication().getConnection();
        if (connection == null) {
            throw new IllegalStateException("The database's authentication aren't connected");
        }

        @NotNull CompletableFuture<Boolean> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                boolean databaseExists = getDatabase().exists().join();

                if (databaseExists) {
                    try (@NotNull ResultSet resultSet = connection.getMetaData().getTables(getDatabase().getId(), null, getName(), null)) {
                        future.complete(resultSet.next());
                        return;
                    }
                }

                future.complete(false);
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }
    public @NotNull CompletableFuture<Long> getAutoIncrement() {
        @Nullable Connection connection = getDatabase().getAuthentication().getConnection();
        if (connection == null) {
            throw new IllegalStateException("The database's authentication aren't connected");
        }

        @NotNull CompletableFuture<Long> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                try (@NotNull PreparedStatement statement = connection.prepareStatement("SHOW TABLE STATUS FROM `" + getDatabase().getId() + "` LIKE '" + getName() + "'")) {
                    @NotNull ResultSet set = statement.executeQuery();
                    set.next();

                    future.complete(set.getLong("auto_increment"));
                }
            } catch (@NotNull Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }

    @Contract(pure = true)
    public @NotNull String getName() {
        return name;
    }

    @Contract(pure = true)
    public @NotNull MysqlDatabase getDatabase() {
        return database;
    }

    public @NotNull Variables getVariables() {
        return variables;
    }
    public @NotNull MysqlData[] getReceptors() {
        return MysqlData.getDatas(this);
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    @Contract(pure = true)
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof MysqlTable)) return false;
        MysqlTable that = (MysqlTable) object;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getDatabase(), that.getDatabase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDatabase());
    }

    @Override
    public @NotNull String toString() {
        return "MysqlTable{" +
                "name='" + name + '\'' +
                ", database=" + database +
                ", variables=" + variables +
                '}';
    }
}
