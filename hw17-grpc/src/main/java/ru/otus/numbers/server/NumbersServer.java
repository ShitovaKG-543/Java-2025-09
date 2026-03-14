package ru.otus.numbers.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.server.service.GrpcNumberServiceImpl;

public class NumbersServer {
    private static final Logger log = LoggerFactory.getLogger(NumbersServer.class);
    private static final int PORT = 8190;

    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        NumbersServer server = new NumbersServer();
        server.start();
        server.blockUntilShutdown();
    }

    private void start() throws IOException {
        server = ServerBuilder.forPort(PORT)
                .addService(new GrpcNumberServiceImpl())
                .build()
                .start();

        log.info("Сервер запущен, прослушивает порт {}", PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Выключение сервера gRPC...");
            this.stop();
            log.info("Завершение работы сервера выполнено");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
