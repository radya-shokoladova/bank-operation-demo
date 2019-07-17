package com.shokoladova.bank_operations;

import spark.Spark;

import java.util.Optional;

import static spark.Spark.*;

public class SparkServer {

    public void start() {
        initPort();
        init();
        awaitInitialization();
        initRoutes();
    }

    private void initRoutes() {
        BankAccountFacade facade = new BankAccountFacadeImpl();

        post("/stop", (req, res) -> {
            this.stop();
            return true;
        });

        path("/account", () -> {

            post("", (req, res) -> facade.create(req));
            get("", (req, res) -> {
                return facade.req(req);
            });
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

    public void stop() {
        Spark.stop();
        System.exit(1);
    }
}
