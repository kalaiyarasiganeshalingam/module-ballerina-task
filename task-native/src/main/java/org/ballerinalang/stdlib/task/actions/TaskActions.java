/*
 *  Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.stdlib.task.actions;

import org.ballerinalang.jvm.BRuntime;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BString;
import org.ballerinalang.stdlib.task.utils.TaskConstants;
import org.ballerinalang.stdlib.task.api.TaskServerConnector;
import org.ballerinalang.stdlib.task.exceptions.SchedulingException;
import org.ballerinalang.stdlib.task.impl.TaskServerConnectorImpl;
import org.ballerinalang.stdlib.task.objects.ServiceInformation;
import org.ballerinalang.stdlib.task.objects.Task;

import static org.ballerinalang.stdlib.task.utils.TaskConstants.NATIVE_DATA_TASK_OBJECT;
import static org.ballerinalang.stdlib.task.utils.Utils.createTaskError;
import static org.ballerinalang.stdlib.task.utils.Utils.processAppointment;
import static org.ballerinalang.stdlib.task.utils.Utils.processTimer;
import static org.ballerinalang.stdlib.task.utils.Utils.validateService;

/**
 * Class to handle ballerina external functions in Task library.
 *
 * @since 0.1.4
 */
public class TaskActions {

    public static Object pause(ObjectValue taskListener) {
        Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
        try {
            task.pause();
        } catch (SchedulingException e) {
            return createTaskError(TaskConstants.SCHEDULER_ERROR, e.getMessage());
        }
        return null;
    }

    public static Object resume(ObjectValue taskListener) {
        Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
        try {
            task.resume();
        } catch (SchedulingException e) {
            return createTaskError(TaskConstants.SCHEDULER_ERROR, e.getMessage());
        }
        return null;
    }

    public static Object detach(ObjectValue taskListener, ObjectValue service) {
        try {
            Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
            String serviceName = service.getType().getName();
            task.removeService(serviceName);
        } catch (Exception e) {
            return createTaskError(TaskConstants.SCHEDULER_ERROR, e.getMessage());
        }
        return null;
    }

    public static Object start(ObjectValue taskListener) {
        Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
        TaskServerConnector serverConnector = new TaskServerConnectorImpl(task);
        try {
            serverConnector.start();
        } catch (SchedulingException e) {
            return createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object stop(ObjectValue taskListener) {
        Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
        TaskServerConnector serverConnector = new TaskServerConnectorImpl(task);
        try {
            serverConnector.stop();
        } catch (SchedulingException e) {
            return createTaskError(e.getMessage());
        }
        return null;
    }

    public static Object attach(ObjectValue taskListener, ObjectValue service, Object... attachments) {
        ServiceInformation serviceInformation;
        if (attachments == null) {
            serviceInformation = new ServiceInformation(BRuntime.getCurrentRuntime(), service);
        } else {
            serviceInformation = new ServiceInformation(BRuntime.getCurrentRuntime(), service, attachments);
        }

        /*
         * TODO: After #14148 fixed, use compiler plugin to validate the service
         */
        try {
            validateService(serviceInformation);
        } catch (SchedulingException e) {
            return createTaskError(e.getMessage());
        }

        Task task = (Task) taskListener.getNativeData(NATIVE_DATA_TASK_OBJECT);
        task.addService(serviceInformation);
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Object init(ObjectValue taskListener) {
        MapValue<BString, Object> configurations = taskListener.getMapValue(TaskConstants.MEMBER_LISTENER_CONFIGURATION);
        String configurationTypeName = configurations.getType().getName();
        Task task;
        try {
            if (TaskConstants.RECORD_TIMER_CONFIGURATION.equals(configurationTypeName)) {
                task = processTimer(configurations);
            } else { // Record type validates at the compile time; Hence we do not need exhaustive validation.
                task = processAppointment(configurations);
            }
            taskListener.addNativeData(NATIVE_DATA_TASK_OBJECT, task);
        } catch (SchedulingException e) {
            return createTaskError(e.getMessage());
        }
        return null;
    }
}
