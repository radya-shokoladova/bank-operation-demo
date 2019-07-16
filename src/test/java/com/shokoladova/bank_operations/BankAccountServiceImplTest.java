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

import static org.junit.Assert.assertEquals;

public class BankAccountServiceImplTest {

    private Dao<BankAccount, UUID> accountDao;
    private ConnectionSource source;
    private BankAccountServiceImpl service;

    @Before
    public void init() throws SQLException {
        service = new BankAccountServiceImpl();
        source = new JdbcPooledConnectionSource("jdbc:h2:mem:revolutDemo");
        accountDao = DaoManager.createDao(source, BankAccount.class);
    }

    @After
    public void teardown() throws SQLException {
        TableUtils.dropTable(source, BankAccount.class, false);
    }

    @Test
    public void create_new_account_with_zero_balance() throws Exception {
        BankAccount account = service.create("shokoladova");

        BankAccount accountFromDatabase = accountDao.queryForId(account.getId());

        assertEquals(account, accountFromDatabase);
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_create_account_with_negative_balance() {
        service.create("shokoladova", -10000);
    }

    @Test
    public void create_new_account_with_some_balance() throws Exception {
        BankAccount account = service.create("shokoladova", 10000);

        BankAccount accountFromDatabase = accountDao.queryForId(account.getId());

        assertEquals(account, accountFromDatabase);
    }

    @Test
    public void get_account_returns_existing_one() {
        BankAccount account = service.create("shokoladova");

        BankAccount returnedAccount = service.get(account.getId());

        assertEquals(account, returnedAccount);
    }

    @Test(expected = BankAccountNotFountException.class)
    public void get_account_throws_exception_when_account_not_found() {
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
    public void can_not_withdraw_zero_value() {
        BankAccount account = service.create("shokoladova", 10000);

        service.withdraw(account.getId(), 0);
    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_withdraw_negative_value() {
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
    public void can_not_deposit_negative_value() {
        BankAccount account = service.create("shokoladova", 10000);

        service.deposit(account.getId(), -100);

    }

    @Test(expected = MoneyOperationException.class)
    public void can_not_deposit_zero_value() {
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

    @Test(expected = MoneyOperationException.class)
    public void transfer_fails_when_have_insufficient_money() {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, sourceAccount.getBalance() * 10);

        service.transfer(transferRequest);
    }

    @Test(expected = BankAccountNotFountException.class)
    public void transfer_fails_when_source_account_does_not_exists() {
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(UUID.randomUUID(), destinationAccountId, 10);

        service.transfer(transferRequest);
    }

    @Test(expected = BankAccountNotFountException.class)
    public void transfer_fails_when_destination_account_does_not_exists() {
        BankAccount sourceAccount = service.create("shokoladova", 10000);

        TransferRequest transferRequest = buildTransferRequest(sourceAccount.getId(), UUID.randomUUID(), 10);

        service.transfer(transferRequest);
    }

    @Test(expected = MoneyOperationException.class)
    public void transfer_fails_when_trying_transfer_zero_value() {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, 0);

        service.transfer(transferRequest);
    }

    @Test(expected = MoneyOperationException.class)
    public void transfer_fails_when_trying_transfer_negative_value() {
        BankAccount sourceAccount = service.create("shokoladova", 10000);
        BankAccount destinationAccount = service.create("marmeladova", 10000);

        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        TransferRequest transferRequest = buildTransferRequest(sourceAccountId, destinationAccountId, -500);

        service.transfer(transferRequest);
    }

    @Test
    public void concurrent_withdraw_successfully_decreases_balance() throws Exception {
        BankAccount account = service.create("shokoladova", 10000);
        CompletableFuture[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> service.withdraw(account.getId(), 1000));
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(futures);
        all.get();

        BankAccount accountFromDb = accountDao.queryForId(account.getId());
        assertEquals(new Integer(0), accountFromDb.getBalance());
    }

    private TransferRequest buildTransferRequest(UUID sourceAccountId, UUID destinationAccountId, Integer amount) {
        return TransferRequest.builder()
                .sourceId(sourceAccountId)
                .destinationId(destinationAccountId)
                .amount(amount)
                .build();
    }
}