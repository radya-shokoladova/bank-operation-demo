package com.shokoladova.bank_operations;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static com.j256.ormlite.misc.TransactionManager.callInTransaction;
import static java.lang.String.format;

public class BankAccountServiceImpl implements BankAccountService {

    private final Dao<BankAccount, UUID> accountDao;

    BankAccountServiceImpl() {
        try {
            JdbcPooledConnectionSource source = getConnectionSource();
            accountDao = DaoManager.createDao(source, BankAccount.class);
            TableUtils.createTableIfNotExists(source, BankAccount.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private JdbcPooledConnectionSource getConnectionSource() throws SQLException {
        return new JdbcPooledConnectionSource("jdbc:h2:mem:revolutDemo");
    }

    @Override
    public BankAccount create(String cardholderName, Integer initialBalance) throws SQLException {
        if (initialBalance < 0) {
            throw new MoneyOperationException("Could not create account with negative balance");
        }
        BankAccount account = new BankAccount(cardholderName, initialBalance);
        accountDao.create(account);

        return account;
    }

    @Override
    public BankAccount get(UUID id) throws SQLException {
        return Optional.ofNullable(accountDao.queryForId(id))
                .orElseThrow(() -> new BankAccountNotFountException(format("Could not find account with id = %s", id)));

    }

    @Override
    public BankAccount withdraw(UUID id, Integer amount) throws SQLException {
        validateAccountExists(id);

        if (amount <= 0) {
            throw new MoneyOperationException("Can not withdraw non-positive value");
        }
        callInTransaction(getConnectionSource(), () -> {
            accountDao.queryRaw("SELECT * FROM ACCOUNT WHERE ID=? FOR UPDATE", id.toString());
            accountDao.executeRaw("UPDATE ACCOUNT SET BALANCE=BALANCE-? WHERE ID=?", amount.toString(), id.toString());
            return true;
        });

        return accountDao.queryForId(id);
    }

    @Override
    public BankAccount deposit(UUID id, Integer amount) throws SQLException {
        validateAccountExists(id);
        if (amount <= 0) {
            throw new MoneyOperationException("Can not deposit non-positive value");
        }
        callInTransaction(getConnectionSource(), () -> {
            accountDao.queryRaw("SELECT * FROM ACCOUNT WHERE ID=? FOR UPDATE", id.toString());
            accountDao.executeRaw("UPDATE ACCOUNT SET BALANCE=BALANCE+? WHERE ID=?", amount.toString(), id.toString());
            return true;
        });

        return accountDao.queryForId(id);
    }

    @Override
    public boolean transfer(TransferRequest transferRequest) throws SQLException {
        validateAccountExists(transferRequest.getSourceId());
        validateAccountExists(transferRequest.getDestinationId());
        if (transferRequest.getAmount() <= 0) {
            throw new MoneyOperationException("Can not transfer non-positive value");
        }

        UUID sourceId = transferRequest.getSourceId();
        UUID destinationId = transferRequest.getDestinationId();

        callInTransaction(getConnectionSource(), () -> {
            BankAccount sourceAccount = accountDao.queryRaw("SELECT * FROM ACCOUNT WHERE ID=? FOR UPDATE", accountDao.getRawRowMapper(), sourceId.toString()).getFirstResult();
            Integer sourceAccountBalance = sourceAccount.getBalance();
            if (sourceAccountBalance < transferRequest.getAmount()) {
                throw new MoneyOperationException("Could not transfer cause insufficient money");
            }
            accountDao.executeRaw("UPDATE ACCOUNT SET BALANCE=BALANCE-? WHERE ID=?", transferRequest.getAmount().toString(), sourceId.toString());
            accountDao.executeRaw("UPDATE ACCOUNT SET BALANCE=BALANCE+? WHERE ID=?", transferRequest.getAmount().toString(), destinationId.toString());
            return true;
        });

        return true;
    }

    private void validateAccountExists(UUID id) throws SQLException {
        if (!accountDao.idExists(id)) {
            throw new BankAccountNotFountException(format("Could not find account with id = %s", id));
        }
    }
}
