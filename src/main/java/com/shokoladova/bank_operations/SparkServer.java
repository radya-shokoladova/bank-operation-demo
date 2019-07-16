package com.shokoladova.bank_operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Spark;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static spark.Spark.*;

public class SparkServer {

    public void start() throws SQLException {
        initPort();
        init();
        awaitInitialization();
        initRoutes();
    }

    private void initRoutes() throws SQLException {
        BankAccountService bankAccountService = new BankAccountServiceImpl();
        ObjectMapper mapper = new ObjectMapper();

        post("/stop", (req, res) -> {
            this.stop();
            return true;
        });

        path("/account", () -> {

            post("", (req, res) -> {
                String name = req.queryParams("cardholderName");
                Optional<Integer> balance = Optional.ofNullable(req.queryParams("balance"))
                        .map(Integer::parseInt);
                return balance.isPresent() ?
                        bankAccountService.create(name, balance.get()):
                        bankAccountService.create(name);
            });
            get("", (req, res) -> {
                UUID id = UUID.fromString(req.queryParams("id"));
                return bankAccountService.get(id);
            });
            post("/withdraw", (req, res) -> {
                String id = req.queryParams("id");
                Integer amount = Integer.valueOf(req.queryParams("amount"));
                return bankAccountService.withdraw(UUID.fromString(id), amount);
            });
            post("/transfer", (req, res) -> {
                TransferRequest transferRequest = mapper.readValue(req.body(), TransferRequest.class);
                return bankAccountService.transfer(transferRequest);
            });
        });


    }

    private void initPort() {
        Integer port = Optional.ofNullable(System.getProperty("port"))
                .map(Integer::parseInt)
                .orElse(8080);
        port(port);
    }

    public void stop() {
        Spark.stop();
        System.exit(1);
    }
}
