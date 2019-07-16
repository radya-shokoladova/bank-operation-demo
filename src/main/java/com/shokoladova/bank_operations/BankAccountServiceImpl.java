package com.shokoladova.bank_operations;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static com.j256.ormlite.misc.TransactionManager.callInTransaction;
import static java.lang.String.format;

public class BankAccountServiceImpl implements BankAccountService {

    private final Dao<BankAccount, UUID> accountDao;
    private final ConnectionSource source;

    public BankAccountServiceImpl() throws SQLException {
        source = new JdbcPooledConnectionSource("jdbc:h2:mem:revolutDemo");
        accountDao = DaoManager.createDao(source, BankAccount.class);
        TableUtils.createTableIfNotExists(source, BankAccount.class);
    }

    @Override
    public BankAccount create(String cardholderName, Integer initialBalance) {
        if (initialBalance < 0) {
            throw new MoneyOperationException("Could not create account with negative balance");
        }
        BankAccount account = new BankAccount(cardholderName, initialBalance);
        try {
            accountDao.create(account);
            return account;
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected database issue", e);
        }
    }

    @Override
    public BankAccount create(String cardholderName) {
        return create(cardholderName, 0);
    }

    @Override
    public BankAccount get(UUID id) {
        try {
            return Optional.ofNullable(accountDao.queryForId(id))
                    .orElseThrow(() -> new BankAccountNotFountException(format("Could not find account with id = %s", id)));
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected database issue", e);
        }
    }

    @Override
    public BankAccount withdraw(UUID id, Integer amount) {
        if (amount <= 0) {
            throw new MoneyOperationException("Can not withdraw non-positive value");
        }
        try {
            synchronized (id.toString().intern()) {
                BankAccount account = accountDao.queryForId(id);
                Integer currentBalance = account.getBalance();
                if (currentBalance < amount) {
                    throw new MoneyOperationException("Could not withdraw cause insufficient money");
                }
                account.setBalance(currentBalance - amount);
                accountDao.update(account);

                return account;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected database issue", e);
        }
    }

    @Override
    public BankAccount deposit(UUID id, Integer amount) {
        if (amount <= 0) {
            throw new MoneyOperationException("Can not deposit non-positive value");
        }
        try {
            synchronized (id.toString().intern()) {
                BankAccount account = accountDao.queryForId(id);
                account.setBalance(account.getBalance() + amount);
                accountDao.update(account);

                return account;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected database issue", e);
        }
    }

    @Override
    public boolean transfer(TransferRequest transferRequest) {
        if (transferRequest.getAmount() <= 0) {
            throw new MoneyOperationException("Can not transfer non-positive value");
        }
        UUID sourceId = transferRequest.getSourceId();
        UUID destinationId = transferRequest.getDestinationId();

        try {
            synchronized (sourceId.toString().intern()) {
                BankAccount sourceAccount = accountDao.queryForId(sourceId);
                BankAccount destinationAccount = accountDao.queryForId(destinationId);

                if (sourceAccount == null || destinationAccount == null) {
                    throw new BankAccountNotFountException(format("One of selected accounts does not exists: %s %s", sourceId, destinationId));
                }

                Integer sourceAccountBalance = sourceAccount.getBalance();
                if (sourceAccountBalance < transferRequest.getAmount()) {
                    throw new MoneyOperationException("Could not transfer cause insufficient money");
                }
                sourceAccount.setBalance(sourceAccountBalance - transferRequest.getAmount());
                destinationAccount.setBalance(destinationAccount.getBalance() + transferRequest.getAmount());
                callInTransaction(source, () -> {
                    accountDao.update(sourceAccount);
                    accountDao.update(destinationAccount);
                    return true;
                });

                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected database issue", e);
        }
    }
}
