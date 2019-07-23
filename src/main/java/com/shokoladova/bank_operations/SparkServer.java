package com.shokoladova.bank_operations;


import java.util.Optional;

import static spark.Spark.*;

class SparkServer {

    private final BankAccountFacade facade = new BankAccountFacadeImpl();

    void start() {
        initPort();
        init();
        awaitInitialization();
        initRoutes();
    }

    private void initRoutes() {

        path("/account", () -> {

            post("", (req, res) -> facade.create(req));
            get("", (req, res) -> facade.get(req));
            post("/withdraw", (req, res) -> facade.withdraw(req));
            post("/transfer", (req, res) -> facade.transfer(req));
            post("/deposit", (req, res) -> facade.deposit(req));
        });


    }

    private void initPort() {
        Integer port = Optional.ofNullable(System.getProperty("port"))
                .map(Integer::parseInt)
                .orElse(8080);
        port(port);
    }
}
