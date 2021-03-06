/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.mgt.authz.service.dao;

import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.persistence.UmPersistenceManager;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.COUNT_COLUMN_NAME;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.ERROR_RETRIEVING_ROOT_ID;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.GET_IS_USER_ALLOWED;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.GET_IS_USER_ALLOWED_AT_LEAST_FOR_ONE_ORG;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.GET_ROOT_ORG_ID;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.PERMISSION_SPLITTER;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.Constants.VIEW_ID;
import static org.wso2.carbon.identity.organization.mgt.authz.service.util.OrganizationMgtAuthzUtil.getUserStoreManager;

public class OrganizationMgtAuthzDAOImpl implements OrganizationMgtAuthzDAO {

    @Override
    public String getRootOrgId(String name, int tenantId) throws OrganizationManagementException {

        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            List<String> ids = jdbcTemplate.executeQuery(GET_ROOT_ORG_ID, (resultSet, rowNumber) ->
                    resultSet.getString(VIEW_ID),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, name);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                    });
            // Query should return only one entry because there is only one ROOT organization.
            return ids.get(0);
        } catch (DataAccessException e) {
            throw new OrganizationManagementException(ERROR_RETRIEVING_ROOT_ID, null, e);
        }
    }

    @Override
    public boolean isUserAuthorized(User user, String resourceId, String action, String orgId, int tenantId)
            throws UserStoreException {

        AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) getUserStoreManager(user);
        String userID = userStoreManager.getUser(null, user.getUserName()).getUserID();
        boolean isUserAllowed;
        String[] permissionParts = resourceId.split(PERMISSION_SPLITTER);
        String parentPermission =
                String.join(PERMISSION_SPLITTER, subArray(permissionParts, 0, permissionParts.length - 1));
        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            int mappingsCount = jdbcTemplate.fetchSingleRecord(GET_IS_USER_ALLOWED,
                    (resultSet, rowNumber) ->
                            resultSet.getInt(COUNT_COLUMN_NAME),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, orgId);
                        preparedStatement.setString(++parameterIndex, userID);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                        preparedStatement.setInt(++parameterIndex, 3);
                        preparedStatement.setString(++parameterIndex, resourceId);
                        preparedStatement.setString(++parameterIndex, parentPermission);
                    });
            isUserAllowed = (mappingsCount > 0);
        } catch (DataAccessException e) {
            //TODO
            throw new UserStoreException(e);
        }
        return isUserAllowed;
    }

    @Override
    public boolean isUserAuthorized(User user, String resourceId, String action, int tenantId)
            throws UserStoreException {

        AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) getUserStoreManager(user);
        if (userStoreManager == null) {
            throw new UserStoreException("Error while retrieving userstore manager for user :" + user.getUserName());
        }
        String userID = userStoreManager.getUser(null, user.getUserName()).getUserID();
        boolean isUserAllowed;
        String[] permissionParts = resourceId.split(PERMISSION_SPLITTER);
        String parentPermission =
                String.join(PERMISSION_SPLITTER, subArray(permissionParts, 0, permissionParts.length - 1));
        JdbcTemplate jdbcTemplate = getNewTemplate();
        try {
            int mappingsCount = jdbcTemplate.fetchSingleRecord(GET_IS_USER_ALLOWED_AT_LEAST_FOR_ONE_ORG,
                    (resultSet, rowNumber) ->
                            resultSet.getInt(COUNT_COLUMN_NAME),
                    preparedStatement -> {
                        int parameterIndex = 0;
                        preparedStatement.setString(++parameterIndex, userID);
                        preparedStatement.setInt(++parameterIndex, tenantId);
                        preparedStatement.setInt(++parameterIndex, 3);
                        preparedStatement.setString(++parameterIndex, resourceId);
                        preparedStatement.setString(++parameterIndex, parentPermission);
                    });
            isUserAllowed = (mappingsCount > 0);
        } catch (DataAccessException e) {
            //TODO
            throw new UserStoreException(e);
        }
        return isUserAllowed;
    }

    public static JdbcTemplate getNewTemplate() {

        return new JdbcTemplate(UmPersistenceManager.getInstance().getDataSource());
    }

    public static <T> T[] subArray(T[] array, int beg, int end) {

        return Arrays.copyOfRange(array, beg, end);
    }
}
