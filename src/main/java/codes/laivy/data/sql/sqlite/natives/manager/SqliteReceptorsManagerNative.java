package codes.laivy.data.sql.sqlite.natives.manager;

import codes.laivy.data.api.variable.container.ActiveVariableContainer;
import codes.laivy.data.sql.SqlVariable;
import codes.laivy.data.sql.manager.SqlReceptorsManager;
import codes.laivy.data.sql.sqlite.SqliteReceptor;
import codes.laivy.data.sql.sqlite.natives.SqliteReceptorNative;
import codes.laivy.data.sql.sqlite.values.SqliteResultData;
import codes.laivy.data.sql.sqlite.values.SqliteResultStatement;
import codes.laivy.data.sql.variable.container.SqlActiveVariableContainer;
import codes.laivy.data.sql.variable.container.SqlActiveVariableContainerImpl;
import codes.laivy.data.sql.variable.container.SqlInactiveVariableContainerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * @author Laivy
 * @since 1.0
 */
public class SqliteReceptorsManagerNative implements SqlReceptorsManager<SqliteReceptor> {
    
    public SqliteReceptorsManagerNative() {
    }

    @Override
    public @Nullable SqliteResultData getData(@NotNull SqliteReceptor receptor) {
        SqliteResultStatement statement = receptor.getDatabase().getConnection().createStatement("SELECT * FROM '" + receptor.getTable().getId() + "' WHERE id = ?");
        statement.getParameters(0).setString(receptor.getId());
        SqliteResultData query = statement.execute();
        statement.close();

        if (query == null) {
            throw new NullPointerException("This result data doesn't have results");
        }

        @NotNull Set<Map<String, Object>> results = query.getValues();
        query.close();

        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return query;
        } else {
            throw new UnsupportedOperationException("Multiples receptors with same id '" + receptor.getId() + "' founded inside table '" + receptor.getTable().getId() + "' at database '" + receptor.getDatabase().getId() + "'");
        }
    }

    @Override
    public void unload(@NotNull SqliteReceptor receptor, boolean save) {
        if (save) {
            save(receptor);
        }
    }

    @Override
    public void save(@NotNull SqliteReceptor receptor) {
        StringBuilder query = new StringBuilder();

        Map<Integer, SqlActiveVariableContainer> indexVariables = new LinkedHashMap<>();

        int row = 1;
        for (ActiveVariableContainer activeVar : receptor.getActiveContainers()) {
            if (activeVar instanceof SqlActiveVariableContainer) {
                SqlActiveVariableContainer container = (SqlActiveVariableContainer) activeVar;

                if (container.getVariable() != null) {
                    if (row != 1) query.append(",");
                    query.append("`").append(container.getVariable().getId()).append("`=?");
                    indexVariables.put(row, container);
                    row++;
                } else {
                    throw new NullPointerException("The active containers of a receptor needs to have a variable!");
                }
            } else {
                throw new IllegalArgumentException("This receptor contains illegal container types");
            }
        }

        SqliteResultStatement statement = receptor.getDatabase().getConnection().createStatement("UPDATE '" + receptor.getTable().getId() + "' SET `index`=?," + query + " WHERE id = ?");
        statement.getParameters(0).setInt(receptor.getIndex());
        statement.getParameters(row).setString(receptor.getId());

        for (Map.Entry<Integer, SqlActiveVariableContainer> map : indexVariables.entrySet()) {
            map.getValue().getType().set(
                    map.getValue().get(),
                    statement.getParameters(map.getKey()),
                    statement.getMetaData()
            );
        }
        statement.execute();
        statement.close();
    }

    @Override
    public void delete(@NotNull SqliteReceptor receptor) {
        SqliteResultStatement statement = receptor.getDatabase().getConnection().createStatement("DELETE FROM '" + receptor.getTable().getId() + "' WHERE id = ?");
        statement.getParameters(0).setString(receptor.getId());
        statement.execute();
        statement.close();
    }

    @Override
    public void load(@NotNull SqliteReceptor receptor) {
        @Nullable SqliteResultData result = getData(receptor);
        Map<String, Object> data = new LinkedHashMap<>();
        if (result != null) {
            data = new LinkedList<>(result.getValues()).getFirst();
            result.close();
        }

        receptor.setNew(data.isEmpty());

        if (data.isEmpty()) {
            // Execute
            SqliteResultStatement statement = receptor.getDatabase().getConnection().createStatement("INSERT INTO '" + receptor.getTable().getId() + "' (id) VALUES (?)");
            statement.getParameters(0).setString(receptor.getId());
            statement.execute();
            statement.close();
            // Data query (again)
            result = getData(receptor);
            if (result != null) {
                data = new LinkedList<>(result.getValues()).getFirst();
                result.close();
            } else {
                throw new NullPointerException("Couldn't create receptor '" + receptor.getId() + "' due to an unknown error.");
            }
        }

        int row = 0;
        for (Map.Entry<String, Object> map : data.entrySet()) {
            if (map.getKey().equals("index")) {
                receptor.setIndex((int) map.getValue());
            } else if (row > 1) { // After index and id columns
                SqlVariable variable = receptor.getTable().getLoadedVariable(map.getKey());
                if (variable != null && variable.isLoaded()) {
                    receptor.getActiveContainers().add(new SqlActiveVariableContainerImpl(variable, receptor, variable.getType().get(map.getValue())));
                } else {
                    receptor.getInactiveContainers().add(new SqlInactiveVariableContainerImpl(map.getKey(), receptor, map.getValue()));
                }
            }
            row++;
        }
    }

    @Override
    public void setId(@NotNull SqliteReceptor receptor, @NotNull String id) {
        if (!id.matches("^.{0,128}$")) {
            throw new IllegalArgumentException("The receptor id must follow the regex '^.{0,128}$'");
        }

        SqliteResultData data = getData(new SqliteReceptorNative(receptor.getTable(), id));
        if (data != null) {
            data.close();
            throw new IllegalArgumentException("A receptor with that id '" + id + "' already exists on the table '" + receptor.getTable() + "'");
        }

        SqliteResultStatement statement = receptor.getDatabase().getConnection().createStatement("UPDATE '" + receptor.getTable().getId() + "' SET id = ? WHERE id = ?");

        statement.getParameters(0).setString(id);
        statement.getParameters(1).setString(receptor.getId());

        statement.execute();
        statement.close();
    }

    @Override
    public void unload(@NotNull SqliteReceptor receptor) {
        this.unload(receptor, true);
    }

    @Override
    public boolean isLoaded(@NotNull SqliteReceptor receptor) {
        return receptor.isLoaded();
    }
}
