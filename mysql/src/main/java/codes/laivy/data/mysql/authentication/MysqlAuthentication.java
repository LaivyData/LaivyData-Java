package codes.laivy.data.mysql.authentication;

import codes.laivy.data.Main;
import codes.laivy.data.mysql.utils.MysqlVersion;
import codes.laivy.data.mysql.database.MysqlDatabase;
import org.jetbrains.annotations.*;

import java.net.InetAddress;
import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MysqlAuthentication {

    private final @NotNull String username;
    private final @Nullable String password;

    private final @NotNull InetAddress hostname;

    @Range(from = 0, to = 65535)
    private final int port;

    private final @NotNull Databases databases;

    @ApiStatus.Internal
    private @Nullable Connection connection;

    protected @Nullable ScheduledExecutorService keepAliveExecutor;
    protected @Nullable MysqlVersion version;

    public MysqlAuthentication(@NotNull String username, @Nullable String password, @NotNull InetAddress hostname, @Range(from = 0, to = 65535) int port) {
        this.username = username;
        this.password = password;
        this.hostname = hostname;
        this.port = port;

        this.databases = new Databases(this);
    }

    @Unmodifiable
    public @NotNull Databases getDatabases() {
        return databases;
    }

    /**
     * Retrieves te connection of this authentication
     * @return The authentication connection or null if isn't authenticated
     */
    public final @Nullable Connection getConnection() {
        return connection;
    }
    public final boolean isConnected() {
        return connection != null;
    }

    @Contract(pure = true)
    public final @NotNull String getUsername() {
        return username;
    }

    @Contract(pure = true)
    public final @Nullable String getPassword() {
        return password;
    }

    @Contract(pure = true)
    public final @NotNull InetAddress getHostname() {
        return hostname;
    }

    @Contract(pure = true)
    @Range(from = 0, to = 65535)
    public final int getPort() {
        return port;
    }

    // TODO: 01/11/2023 Javadoc
    protected boolean isAutoCommit() {
        return true;
    }

    /**
     * Connects to the mysql with the provided authentication details.
     *
     * @return A CompletableFuture representing the asynchronous start operation
     * @throws IllegalStateException If the database is already loaded
     * @since 2.0
     */
    public final @NotNull CompletableFuture<Connection> connect() {
        if (isConnected()) {
            throw new IllegalStateException("This authentication already are connected!");
        }

        @NotNull CompletableFuture<Connection> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                Class<Driver> ignore = getDriver();
                this.connection = load().get(10, TimeUnit.SECONDS);
                this.connection.setAutoCommit(isAutoCommit());

                @NotNull DatabaseMetaData metadata = getConnection().getMetaData();
                version = MysqlVersion.of(metadata.getDatabaseMinorVersion(), metadata.getDatabaseMajorVersion(), metadata.getDatabaseProductVersion());

                for (@NotNull MysqlDatabase database : loadDatabases(metadata)) {
                    getDatabases().add(database);
                }

                future.complete(connection);
            } catch (Throwable throwable) {
                try {
                    if (isConnected()) {
                        disconnect().get(3, TimeUnit.SECONDS);
                    }
                } catch (Throwable ignore) {
                }

                future.completeExceptionally(throwable);
            }
        }, Main.getExecutor(getClass()));

        return future;
    }

    @Blocking
    @ApiStatus.Internal
    private @NotNull Set<MysqlDatabase> loadDatabases(@NotNull DatabaseMetaData metaData) throws Throwable {
        @NotNull ResultSet set = metaData.getCatalogs();
        @NotNull Set<MysqlDatabase> databases = new LinkedHashSet<>();

        while (set.next()) {
            @NotNull String databaseName = set.getString(1);
            @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(this, databaseName);
            databases.add(database);
        }

        return databases;
    }

    public final @NotNull MysqlVersion getVersion() {
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("To retrieve mysql version the authentication needs to be connected!");
        }
    }

    public @NotNull Class<Driver> getDriver() {
        try {
            try {
                //noinspection unchecked
                return (Class<Driver>) Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                //noinspection unchecked
                return (Class<Driver>) Class.forName("com.mysql.jdbc.Driver");
            }
        } catch (Throwable e) {
            throw new RuntimeException("Cannot load default driver 'com.mysql.jdbc.Driver'", e);
        }
    }

    /**
     * Stops the database.
     *
     * @return A CompletableFuture representing the asynchronous stop operation
     * @throws IllegalStateException If the database is not loaded
     * @since 2.0
     */
    public final @NotNull CompletableFuture<Void> disconnect() {
        if (!isConnected()) {
            throw new IllegalStateException("This authentication aren't connected");
        }

        @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                unload().join();

                for (MysqlDatabase database : getDatabases()) {
                    if (database.isLoaded()) {
                        database.stop().join();
                    }
                }
                getDatabases().clear();

                connection.close();
                connection = null;
                version = null;

                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, Main.getExecutor(getClass()));

        return future;
    }

    /**
     * Loads the authentication, preparing it for operations and authenticating to the server.
     *
     * @return A CompletableFuture representing the asynchronous load operation
     * @since 1.0
     */
    @ApiStatus.OverrideOnly
    protected @NotNull CompletableFuture<Connection> load() {
        @NotNull CompletableFuture<Connection> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                @NotNull Connection connection = DriverManager.getConnection("jdbc:mysql://" + getHostname().getHostAddress() + ":" + getPort() + "/?autoReconnect=true&failOverReadOnly=false&verifyServerCertificate=false", getUsername(), getPassword());
                connection.setNetworkTimeout(Executors.newFixedThreadPool(1), (int) TimeUnit.MINUTES.toMillis(30));

                keepAliveExecutor = Executors.newScheduledThreadPool(1);
                keepAliveExecutor.scheduleAtFixedRate(this::checkConnection, 0, 4, TimeUnit.MINUTES);

                future.complete(connection);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, Main.getExecutor(getClass()));

        return future;
    }

    /**
     * Unloads the authentication, releasing resources.
     *
     * @return A CompletableFuture representing the asynchronous unload operation
     * @since 1.0
     */
    @ApiStatus.OverrideOnly
    protected @NotNull CompletableFuture<Void> unload() throws SQLException {
        if (keepAliveExecutor == null || connection == null) {
            throw new IllegalStateException("The authentication hasn't started correctly");
        } else if (connection.isClosed()) {
            throw new IllegalStateException("The connection aren't open");
        } else {
            @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

            CompletableFuture.runAsync(() -> {
                try {
                    keepAliveExecutor.shutdown();
                    keepAliveExecutor = null;

                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }

                    future.complete(null);
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            }, Main.getExecutor(getClass()));

            return future;
        }
    }

    /**
     * Checks the connection's validity and attempts reconnection if necessary.
     * <p>
     * This method is called periodically to verify the connection's state.
     * If the connection is closed or not valid, it attempts reconnection within a timeout.
     *
     * @throws RuntimeException If the connection is closed by the server-side or is no longer valid
     * @since 1.0
     */
    protected void checkConnection() {
        if (isConnected()) {
            try {
                boolean valid = getConnection().isValid(2);

                if (!valid) {
                    reconnect().get(5, TimeUnit.SECONDS);
                }
            } catch (Throwable throwable) {
                throw new RuntimeException("Connection closed by server-side or is not valid anymore", throwable);
            }
        }
    }

    public @NotNull CompletableFuture<Void> reconnect() {
        @NotNull CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                if (isConnected()) {
                    disconnect().get(5, TimeUnit.SECONDS);
                }
                connect().get(5, TimeUnit.SECONDS);

                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, Main.getExecutor(getClass()));

        return future;
    }

}
