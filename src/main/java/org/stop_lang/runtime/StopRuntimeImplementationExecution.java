package org.stop_lang.runtime;

import org.stop_lang.stop.validation.StopValidationException;

public interface StopRuntimeImplementationExecution<T> {
    void queue(T implementationInstance) throws StopRuntimeException, StopValidationException;
    void log(String message);
}
