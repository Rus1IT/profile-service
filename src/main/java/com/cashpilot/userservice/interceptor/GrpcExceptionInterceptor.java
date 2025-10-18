package com.cashpilot.userservice.interceptor;

import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@GrpcGlobalServerInterceptor
public class GrpcExceptionInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (ValidationException e) {
                    log.warn("Validation error: {}", e.getMessage());
                    closeWithStatus(call, Status.INVALID_ARGUMENT.withDescription(e.getMessage()), e);
                } catch (NotFoundException e) {
                    log.warn("Not found: {}", e.getMessage());
                    closeWithStatus(call, Status.NOT_FOUND.withDescription(e.getMessage()), e);
                } catch (AlreadyExistException e) {
                    log.warn("Already exists: {}", e.getMessage());
                    closeWithStatus(call, Status.ALREADY_EXISTS.withDescription(e.getMessage()), e);
                } catch (StatusRuntimeException e) {
                    // Если сервис сам уже выбросил корректный статус — не трогаем
                    log.debug("gRPC status exception: {}", e.getStatus());
                    call.close(e.getStatus(), new Metadata());
                } catch (Exception e) {
                    // Любая непредвиденная ошибка
                    log.error("Unexpected internal error", e);
                    closeWithStatus(call, Status.INTERNAL.withDescription("Internal server error"), e);
                }
            }
        };
    }

    private <RespT> void closeWithStatus(ServerCall<?, RespT> call, Status status, Throwable t) {
        call.close(status.withCause(t), new Metadata());
    }
}