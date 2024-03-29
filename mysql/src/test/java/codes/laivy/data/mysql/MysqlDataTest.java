package codes.laivy.data.mysql;

import codes.laivy.data.mysql.authentication.MysqlAuthentication;
import codes.laivy.data.mysql.data.Condition;
import codes.laivy.data.mysql.data.MysqlData;
import codes.laivy.data.mysql.data.MysqlDataCache;
import codes.laivy.data.mysql.database.MysqlDatabase;
import codes.laivy.data.mysql.table.MysqlTable;
import codes.laivy.data.mysql.variable.MysqlVariable;
import codes.laivy.data.mysql.variable.type.provider.MysqlIntType;
import codes.laivy.data.mysql.variable.type.provider.MysqlTextType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MysqlDataTest {

    public final @NotNull String USERNAME;
    public final @NotNull String PASSWORD;
    public final @NotNull InetAddress ADDRESS;
    public final int PORT;

    public MysqlDataTest() throws Throwable {
        PASSWORD = "";
        USERNAME = "root";
        PORT = 3306;
        ADDRESS = InetAddress.getByName("localhost");
    }

    @Test
    public void testLoadAndUnload() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), null);
        variable.start().get(2, TimeUnit.SECONDS);
        Assert.assertTrue(variable.isNew());

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        Assert.assertFalse(data.exists().get(2, TimeUnit.SECONDS));

        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertFalse(data.exists().get(2, TimeUnit.SECONDS));
        Assert.assertTrue(data.isLoaded());

        data.stop(true).get(2, TimeUnit.SECONDS);
        Assert.assertTrue(data.exists().get(2, TimeUnit.SECONDS));
        Assert.assertFalse(data.isLoaded());
        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertTrue(data.isLoaded());
        //

        database.delete().get(2, TimeUnit.SECONDS);

        Assert.assertFalse(data.exists().get(2, TimeUnit.SECONDS));
        Assert.assertFalse(data.isLoaded());

        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testValueSet() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), "a");
        variable.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertTrue(data.isLoaded());
        Assert.assertEquals(data.get(variable), "a");

        @NotNull String expected = "Just a cool test :)";
        data.set(variable, expected);
        data.stop(true).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertEquals(expected, data.get(variable));
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testDefaultValue() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertTrue(data.isNew());
        Assert.assertEquals(expected, data.get(variable));
        data.stop(true).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        Assert.assertFalse(data.isNew());
        Assert.assertEquals(expected, data.get(variable));
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testWithoutVariables() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        data.stop(true).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testCondition() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        int row = data.getRow();

        Assert.assertTrue(data.isNew());
        Assert.assertEquals(expected, data.get(variable));
        data.stop(true).get(2, TimeUnit.SECONDS);
        //

        @NotNull MysqlData[] datas = MysqlData.retrieve(table, Condition.of(variable, expected)).get(2, TimeUnit.SECONDS);
        Assert.assertTrue(datas.length == 1 && datas[0].getRow() == row);

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testDelete() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        data.save().get(2, TimeUnit.SECONDS);
        Assert.assertTrue(data.exists().get(2, TimeUnit.SECONDS));
        data.delete().get(2, TimeUnit.SECONDS);
        Assert.assertFalse(data.exists().get(2, TimeUnit.SECONDS));
        Assert.assertFalse(data.isLoaded());
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testAddVariableAfterDataLoading() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);

        Assert.assertEquals(data.get(variable), expected);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testStartVariableAfterDataLoading() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);
        variable.stop().get(2, TimeUnit.SECONDS);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);

        variable.start().get(2, TimeUnit.SECONDS);

        Assert.assertEquals(data.get(variable), expected);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
    @Test
    public void testStartVariableAfterDataWithoutCreatingLoading() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);

        variable.start().get(2, TimeUnit.SECONDS);

        Assert.assertEquals(data.get(variable), expected);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
    @Test
    public void testLoadVariableWithoutAData() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);

        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);

        // Data code
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        data.stop(true).get(2, TimeUnit.SECONDS);

        variable.start().get(2, TimeUnit.SECONDS);

        data.start().get(2, TimeUnit.SECONDS);

        Assert.assertEquals(data.get(variable), expected);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testExistsWithCondition() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";
        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);

        // Creating 4 datas
        MysqlData.create(table).get(2, TimeUnit.SECONDS).start().get(2, TimeUnit.SECONDS);
        MysqlData.create(table).get(2, TimeUnit.SECONDS).start().get(2, TimeUnit.SECONDS);
        MysqlData.create(table).get(2, TimeUnit.SECONDS).start().get(2, TimeUnit.SECONDS);
        // I'll unload that one just to improve test accuracy
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        data.stop(true).get(2, TimeUnit.SECONDS);
        // Verifying if exists the 4 datas
        Assert.assertEquals((Integer) 4, MysqlData.exists(table, Condition.of(variable, expected)).get(2, TimeUnit.SECONDS));
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testGlobalRetrieve() throws Exception {
        @NotNull Random random = new Random();
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");
        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        // Creating some data
        int amount = random.nextInt(100);
        for (int row = 0; row < amount; row++) {
            @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
            data.save().get(2, TimeUnit.SECONDS);

            if (random.nextBoolean()) {
                data.start().get(2, TimeUnit.SECONDS);
            }
        }

        // Verifying if exists all the data
        Assert.assertEquals((Integer) amount, (Integer) MysqlData.retrieve(table).get(2, TimeUnit.SECONDS).length);
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testUnloadedSave() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected1 = "Just a cool test :)";
        @NotNull String expected2 = "Another cool test :)";

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var1", table, new MysqlTextType(), expected1);
        variable.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<String> variable2 = new MysqlVariable<>("test_var2", table, new MysqlTextType(), expected2);
        variable2.start().get(2, TimeUnit.SECONDS);

        // Creating 4 datas
        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.save().get(2, TimeUnit.SECONDS);
        // Verifying if exists the 4 datas
        Assert.assertEquals((Integer) 1, MysqlData.exists(table, Condition.of(variable, expected1), Condition.of(variable2, expected2)).get(2, TimeUnit.SECONDS));
        //

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testCaching() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected1 = "Just a cool test :)";

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var1", table, new MysqlTextType(), expected1);
        variable.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
        data.start().get(2, TimeUnit.SECONDS);
        data.set(variable, "ata");

        data.stop(true).get(2, TimeUnit.SECONDS);
        table.getDataContent().remove(data);

        variable.stop().get(2, TimeUnit.SECONDS);
        variable.start().get(2, TimeUnit.SECONDS);

        data = MysqlData.retrieve(table, Condition.of(variable, "ata")).get(2, TimeUnit.SECONDS)[0];
        data.start().get(2, TimeUnit.SECONDS);

        Assert.assertEquals(data.get(variable), "ata");
        Assert.assertTrue(data.matches(Condition.of(variable, "ata")));

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testMultiples() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var1", table, new MysqlTextType(), null);
        variable.start().get(2, TimeUnit.SECONDS);

        int total = 0;
        for (int row = 0; row < 100; row++) {
            @NotNull MysqlData data = MysqlData.create(table).get(2, TimeUnit.SECONDS);
            data.start().get(2, TimeUnit.SECONDS);

            if (new Random().nextBoolean()) {
                data.set(variable, expected);
                total++;
            }

            data.stop(true).get(2, TimeUnit.SECONDS);
            table.getDataContent().remove(data);
        }

        Assert.assertEquals(total, MysqlData.retrieve(table, Condition.of(variable, expected)).join().length);

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testStaticSetter() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var1", table, new MysqlTextType(), null);
        variable.start().get(2, TimeUnit.SECONDS);
        @NotNull MysqlVariable<Integer> variable2 = new MysqlVariable<>("test_var2", table, new MysqlIntType(), 0);
        variable2.start().get(2, TimeUnit.SECONDS);

        // First test, with data started
        @NotNull MysqlData data = MysqlData.create(table).join();
        data.start().join();
        MysqlData.set(variable, expected, Condition.of(variable2, 0)).join();
        Assert.assertEquals(data.get(variable), expected);
        data.delete().join();
        data.create().join();
        // Second test, with data stopped
        MysqlData.set(variable, expected, Condition.of(variable2, 0)).join();
        data.start().join();
        Assert.assertEquals(data.get(variable), expected);
        data.stop(false).join();
        // Second test with row
        expected = "Just another cool test :)";
        MysqlData.set(variable, expected, data.getRow()).join();
        data.start().join();
        Assert.assertEquals(data.get(variable), expected);
        // Finished

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
    @Test
    public void testStaticGetter() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull String expected = "Just a cool test :)";

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("test_var", table, new MysqlTextType(), expected);
        variable.start().get(2, TimeUnit.SECONDS);

        // First test, with data started
        @NotNull MysqlData data = MysqlData.create(table).join();
        data.create().join();

        Assert.assertEquals(MysqlDataCache.get(variable, data.getRow()).join(), expected);
        // Finished

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
    @Test
    public void testStaticDelete() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlData data = MysqlData.create(table).join();
        data.start().join();
        data.stop(true).join();
        data.start().join();

        Assert.assertTrue(data.exists().join());
        MysqlData.delete(data.getTable(), data.getRow()).join();
        Assert.assertFalse(data.exists().join());
        Assert.assertFalse(data.isLoaded());

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
    @Test
    public void testStaticDeleteCondition() throws Exception {
        @NotNull MysqlAuthentication authentication = new MysqlAuthentication(USERNAME, PASSWORD, ADDRESS, PORT);
        authentication.connect().get(5, TimeUnit.SECONDS);
        @NotNull MysqlDatabase database = MysqlDatabase.getOrCreate(authentication, "test");

        database.start().get(2, TimeUnit.SECONDS);
        database.delete().get(2, TimeUnit.SECONDS);
        database.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlTable table = new MysqlTable("test_table", database);
        table.start().get(2, TimeUnit.SECONDS);

        @NotNull MysqlVariable<String> variable = new MysqlVariable<>("variable", table, new MysqlTextType(MysqlTextType.Size.TINYTEXT), "test");
        variable.start().join();

        List<MysqlData> datas = new LinkedList<>();
        List<MysqlData> contains = new LinkedList<>();
        for (int row = 0; row < 15; row++) {
            @NotNull MysqlData data = MysqlData.create(table).join();
            data.create().join();
            data.start().join();

            datas.add(data);
            if (Math.random() > 0.5D) {
                data.set(variable, null);
                contains.add(data);
            }
        }

        MysqlData.delete(table, Condition.of(variable, variable.getDefaultValue())).join();

        for (MysqlData data : datas) {
            if (contains.contains(data)) {
                Assert.assertTrue(data.isLoaded());
                Assert.assertTrue(data.exists().join());
            } else {
                Assert.assertFalse(data.isLoaded());
                Assert.assertFalse(data.exists().join());
            }
        }

        database.delete().get(2, TimeUnit.SECONDS);
        authentication.disconnect().get(5, TimeUnit.SECONDS);
    }
}
