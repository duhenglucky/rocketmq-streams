/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.streams.common.datatype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.streams.common.utils.CollectionUtil;
import org.apache.rocketmq.streams.common.utils.ContantsUtil;
import org.apache.rocketmq.streams.common.utils.DataTypeUtil;
import org.apache.rocketmq.streams.common.utils.StringUtil;

public class ListDataType extends GenericParameterDataType<List> {

    private static final long serialVersionUID = -2590322335704835947L;
    private transient DataType paradigmType;

    public ListDataType(Class clazz, DataType paradigmType) {
        setDataClazz(clazz);
        this.paradigmType = paradigmType;
        this.setGenericParameterStr(createGenericParameterStr());
    }

    public ListDataType(DataType paradigmType) {
        setDataClazz(List.class);
        this.paradigmType = paradigmType;
        this.setGenericParameterStr(createGenericParameterStr());
    }

    public ListDataType() {
        setDataClazz(List.class);
    }

    @Override
    public String toDataStr(List value) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean isFirst = true;
        if (CollectionUtil.isNotEmpty(value)) {
            for (Object object : value) {
                if (object == null) {
                    continue;
                }
                String str = paradigmType.toDataJson(object);
                if (str == null) {
                    continue;
                }
                if (isFirst) {
                    isFirst = false;
                } else {
                    stringBuilder.append(",");
                }
                if (str.indexOf(",") != -1) {
                    stringBuilder.append("'" + str + "'");
                } else {
                    stringBuilder.append(str);
                }

            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String toDataJson(List value) {
        if (JSONArray.class.isInstance(value)) {
            return ((JSONArray)value).toJSONString();
        }
        JSONArray jsonArray = new JSONArray();
        if (CollectionUtil.isNotEmpty(value)) {
            for (Object object : value) {
                jsonArray.add(paradigmType.toDataJson(object));
            }
        }
        return jsonArray.toJSONString();
    }

    @Override
    public void setDataClazz(Class dataClazz) {
        this.dataClazz = List.class;
    }

    @Override
    public List getData(String jsonValue) {
        if (StringUtil.isEmpty(jsonValue)) {
            return null;
        }
        if (isQuickModel(jsonValue)) {
            jsonValue = createJsonValue(jsonValue);
        }
        JSONArray jsonArray = JSON.parseArray(jsonValue);
        List list = new ArrayList();
        for (int i = 0; i < jsonArray.size(); i++) {
            String json = jsonArray.getString(i);
            Object result = json;
            if (paradigmType != null) {
                result = paradigmType.getData(json);
            }
            list.add(result);
        }
        return list;
    }

    private String createJsonValue(String jsonValue) {
        String value = jsonValue;
        Map<String, String> flag2ExpressionStr = new HashMap<>();
        boolean containsContant = ContantsUtil.containContant(jsonValue);
        if (containsContant) {
            value = ContantsUtil.doConstantReplace(jsonValue, flag2ExpressionStr, 1);
        }
        JSONArray jsonArray = new JSONArray();
        String[] values = value.split(",");
        for (int i = 0; i < values.length; i++) {
            String tmp = values[i];
            if (containsContant) {
                tmp = ContantsUtil.restore(tmp, flag2ExpressionStr);
                if (ContantsUtil.isContant(tmp)) {
                    tmp = tmp.substring(1, tmp.length() - 1);
                }

            }
            jsonArray.add(tmp);
        }
        return jsonArray.toJSONString();
    }

    protected boolean isQuickModel(String jsonValue) {
        if (StringUtil.isEmpty(jsonValue)) {
            return false;
        }
        return !jsonValue.trim().startsWith("{") && !jsonValue.trim().startsWith("[") && paradigmType.matchClass(String.class);
    }

    public static String getTypeName() {
        return "set";
    }

    @Override
    public boolean matchClass(Class clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    public DataType create() {
        return new ListDataType();
    }

    @Override
    public String getDataTypeName() {
        return getTypeName();
    }

    public List convertValue(ArrayList value) {
        if (value == null) {
            return null;
        }
        return (List)value;
    }

    @Override
    public void parseGenericParameter(String genericParameterString) {
        if (StringUtil.isEmpty(genericParameterString)) {
            return;
        }
        genericParameterString = genericParameterString.trim();
        int index = List.class.getName().length() + 1;
        String subClassString = genericParameterString.substring(index, genericParameterString.length() - 1);
        index = subClassString.indexOf("<");
        if (index != -1) {
            String className = subClassString.substring(0, index);
            Class clazz = createClass(className);
            DataType dataType = DataTypeUtil.getDataTypeFromClass(clazz);
            if (GenericParameterDataType.class.isInstance(dataType)) {
                GenericParameterDataType tmp = (GenericParameterDataType)dataType;
                tmp.parseGenericParameter(subClassString);
            }

            this.paradigmType = dataType;
        } else {
            Class clazz = createClass(subClassString);
            this.paradigmType = DataTypeUtil.getDataTypeFromClass(clazz);
        }
    }

    @Override
    protected String createGenericParameterStr() {
        String subStr = createGenericParameterStr(paradigmType);
        return List.class.getName() + "<" + subStr + ">";
    }

    public static void main(String[] args) {
        ListDataType listDataType = new ListDataType(new StringDataType());
        List<String> list = listDataType.getData("[\"fdsdfds\",\"dfs\"]");
        list = listDataType.getData(listDataType.toDataStr(list));
        System.out.println(listDataType.toDataStr(list));
        //        listDataType.parseGenericParameter(
        //            "java.util.List M<java.lang.String>");//"java.util.List<java.util.Map<java.util.Map<java.lang.String,java
        //        // .util.List<java.lang.String>>,java.util.List<java.util.Map<java.lang.String,"+Integer.class.getName()
        //        // +">>>>");
        //        System.out.println(listDataType.createGenericParameterStr());
    }

    public void setParadigmType(DataType paradigmType) {
        this.paradigmType = paradigmType;
    }
}
