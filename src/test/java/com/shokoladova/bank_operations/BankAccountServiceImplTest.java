package com.shokoladova.bank_operations;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class BankAccountServiceImplTest {

    private Dao<BankAccount, UUID> accountDao;
    private ConnectionSource source;
    private BankAccountServiceImpl service = new BankAccountServiceImpl();

    public BankAccountServiceImplTest() throws SQLException {
    }

    @Before
    public void init() throws SQLException {
        source = new JdbcPooledConnectionSource("jdbc:h2:mem:revolutDemo");
        accountDao = DaoManager.createDao(source, BankAccount.class);
    }

    @After
    public void teardown() throws SQLException {
        TableUtils.clearTable(source, BankAccount.class);
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_create_account_with_negative_balance() throws SQLException {
        service.create("shokoladova", -10000);
    }

    @Test
    public void create_new_account_with_some_balance() throws Exception {
        BankAccount account = service.create("shokoladova", 10000);

        BankAccount accountFromDatabase = accountDao.queryForId(account.getId());

        assertEquals(account, accountFromDatabase);
    }

    @Test
    public void get_account_returns_existing_one() throws SQLException {
        BankAccount account = service.create("shokoladova", 0);

        BankAccount returnedAccount = service.get(account.getId());

        assertEquals(account, returnedAccount);
    }

    @Test(expected = BankAccountNotFountException.class)
    public void get_account_throws_exception_when_account_not_found() throws SQLException {
        service.get(UUID.randomUUID());
    }

    @Test
    public void withdraw_successfully_decreases_balnace() throws Exception {
        BankAccount account = service.create("shokoladova", 10000);

        service.withdraw(account.getId(), 10000);

        BankAccount accountFromDb = accountDao.queryForId(account.getId());
        assertEquals(new Integer(0), accountFromDb.getBalance());
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_withdraw_zero_value() throws SQLException {
        BankAccount account = service.create("shokoladova", 10000);

        service.withdraw(account.getId(), 0);
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_withdraw_negative_value() throws SQLException {
        BankAccount account = service.create("shokoladova", 10000);

        service.withdraw(account.getId(), 0);
    }

    @Test
    public void deposit_successfully_increases_balnace() throws Exception {
        BankAccount account = service.create("shokoladova", 10000);

        service.deposit(account.getId(), 10000);

        BankAccount accountFromDb = accountDao.queryForId(account.getId());
        assertEquals(new Integer(20000), accountFromDb.getBalance());
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_deposit_negative_value() throws SQLException {
        BankAccount account = service.create("shokoladova", 10000);

        service.deposit(account.getId(), -100);

    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_deposit_zero_value() throws SQLException {
        BankAccount account = service.create("shokoladova", 10000);

        service.deposit(account.getId(), 0);
    }

    @Test
    public void transfer_successfully_transfers_money() throws Exception {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, sourceAccount.getBalance());

        service.transfer(transferRequest);

        BankAccount sourceAccountFromDb = accountDao.queryForId(sourceAccountId);
        BankAccount destinationAccountFromDb = accountDao.queryForId(destinationAccountId);
        assertEquals(new Integer(0), sourceAccountFromDb.getBalance());
        assertEquals(new Integer(20000), destinationAccountFromDb.getBalance());
    }

    @Test
    public void transfer_fails_when_have_insufficient_money() throws SQLException {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, sourceAccount.getBalance() * 10);

        try {
            service.transfer(transferRequest);
        } catch (SQLException e) {
            assertEquals(e.getCause().getClass(), MoneyOperationException.class);
        }
    }

    @Test(expected = BankAccountNotFountException.class)
    public void transfer_fails_when_source_account_does_not_exists() throws Exception {
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(UUID.randomUUID(), destinationAccountId, 10);

        service.transfer(transferRequest);
    }

    @Test(expected = BankAccountNotFountException.class)
    public void transfer_fails_when_destination_account_does_not_exists() throws SQLException {
        BankAccount sourceAccount = service.create("shokoladova", 10000);

        TransferRequest transferRequest = buildTransferRequest(sourceAccount.getId(), UUID.randomUUID(), 10);

        service.transfer(transferRequest);
    }

    @Test(expected = MoneyOperationException.class)
    public void transfer_fails_when_trying_transfer_zero_value() throws SQLException {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, 0);

        service.transfer(transferRequest);
    }

    @Test(expected = MoneyOperationException.class)
    public void transfer_fails_when_trying_transfer_negative_value() throws SQLException {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, -500);

        service.transfer(transferRequest);
    }

    @Test
    public void concurrent_withdraw_and_deposit_successfully_applies_correctly() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        BankAccount account = service.create("shokoladova", 10000);
        CompletableFuture[] futures = new CompletableFuture[20];
        for (int i = 0; i < 10; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.withdraw(account.getId(), 1000);
                } catch (SQLException e) {
                    return false;
                }
            }, executorService);
            futures[futures.length - i - 1] = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.deposit(account.getId(), 500);
                } catch (SQLException e) {
                    return false;
                }
            }, executorService);
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(futures);
        all.get();

        BankAccount accountFromDb = accountDao.queryForId(account.getId());
        assertEquals(new Integer(5000), accountFromDb.getBalance());
    }

    @Test
    public void concurrent_bidirectional_transfer_is_ok() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);
        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, 500);
        TransferRequest inversedTransferRequest = buildTransferRequest(destinationAccountId, sourceAccountId, 1000);

        CompletableFuture[] futures = new CompletableFuture[20];
        for (int i = 0; i < 10; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.transfer(transferRequest);
                } catch (SQLException e) {
                    return false;
                }
            }, executorService);
            futures[futures.length - i - 1] = CompletableFuture.supplyAsync(() -> {
                try {
                    return service.transfer(inversedTransferRequest);
                } catch (SQLException e) {
                    return false;
                }
            }, executorService);
        }
        CompletableFuture.allOf(futures).get();

        BankAccount sourceAccountFromDb = accountDao.queryForId(sourceAccountId);
        BankAccount destinationAccountFromDb = accountDao.queryForId(destinationAccountId);
        assertEquals(new Integer(15000), sourceAccountFromDb.getBalance());
        assertEquals(new Integer(5000), destinationAccountFromDb.getBalance());
    }

    private TransferRequest buildTransferRequest(UUID sourceAccountId, UUID destinationAccountId, Integer amount) {
        return TransferRequest.builder()
                .sourceId(sourceAccountId)
                .destinationId(destinationAccountId)
                .amount(amount)
                .build();
    }
}