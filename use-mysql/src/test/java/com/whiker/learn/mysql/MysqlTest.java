package com.whiker.learn.mysql;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class MysqlTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlTest.class);

    private Connection mConn = ConnectionManager.getConnection();

    @Before
    public void before() {
        createTableUser();
    }

    @After
    public void after() {
        dropTableUser();
    }

    @Test
    public void test() {
        User u1 = new User("steven", (short) 20, User.Gender.Man);
        User u2 = new User("ada", (short) 20, User.Gender.Woman);

        Assert.assertTrue(insertUser(u1));
        Assert.assertTrue(insertUser(u2));
//        Assert.assertFalse(insertUser(u1));

        User us1 = selectUserByName(u1.getName());
        Assert.assertNotNull(us1);
        Assert.assertTrue(us1.equals(u1));
        Assert.assertFalse(us1.equals(u2));

        User us2 = selectUserByName(u2.getName());
        Assert.assertNotNull(us2);
        Assert.assertTrue(us2.equals(u2));
    }

    /**
     * 创建表-user
     */
    private void createTableUser() {
        try (Statement stmt = mConn.createStatement()) {
            String sql = "create table user(" +
                    "id int not null auto_increment primary key," +
                    "name varchar(20) not null unique key," +
                    "age tinyint unsigned," +
                    "gender enum('0','1','2') default '0')";  // 不以分号结尾
            if (stmt.executeUpdate(sql) == -1) {
                LOGGER.error("创建表-user失败");
            }
        } catch (SQLException e) {
            LOGGER.error("创建表-user失败", e);
        }
    }

    /**
     * 删除表-user
     */
    private void dropTableUser() {
        try (Statement stmt = mConn.createStatement()) {
            if (stmt.executeUpdate("drop table user") == -1) {
                LOGGER.error("删除表-user失败");
            }
        } catch (SQLException e) {
            LOGGER.error("删除表-user失败", e);
        }
    }

    /**
     * 向表-user插入一条记录
     *
     * @param user user实体
     */
    private boolean insertUser(User user) {
        String sql = "insert into user(name, age, gender) values(?, ?, ?)";
        try (PreparedStatement pstmt = mConn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setShort(2, user.getAge());
            pstmt.setString(3, user.getGender().toString());
            if (pstmt.executeUpdate() == -1) {
                LOGGER.error("插入表-user失败", "user: name={}, age={}, gender={}",
                        new Object[]{user.getName(), user.getAge(), user.getGender()});
                return false;
            }
            return true;
        } catch (SQLException e) {
            LOGGER.error("插入表-user失败", e);
            return false;
        }
    }

    /**
     * 通过名字查找user实体
     *
     * @param name user的name
     * @return 查找结果
     */
    private User selectUserByName(String name) {
        String sql = "select * from user where name = ?";
        try (PreparedStatement pstmt = mConn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            User user = null;
            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt(1));
                user.setName(rs.getString(2));
                user.setAge(rs.getShort(3));
                user.setGender(rs.getString(4));
            }
            rs.close();
            return user;
        } catch (SQLException e) {
            LOGGER.error("查找表-user失败", e);
            return null;
        }
    }
}
