package com.java110.api.listener.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.api.bmo.owner.IOwnerBMO;
import com.java110.api.bmo.room.IRoomBMO;
import com.java110.api.listener.AbstractServiceApiDataFlowListener;
import com.java110.core.smo.owner.IOwnerCarInnerServiceSMO;
import com.java110.core.smo.room.IRoomInnerServiceSMO;
import com.java110.dto.RoomDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.utils.constant.*;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.core.annotation.Java110Listener;
import com.java110.core.context.DataFlowContext;
import com.java110.core.smo.community.ICommunityInnerServiceSMO;
import com.java110.dto.CommunityMemberDto;
import com.java110.entity.center.AppService;
import com.java110.event.service.api.ServiceDataFlowEvent;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiRoomVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 删除小区楼信息
 */
@Java110Listener("deleteOwnerListener")
public class DeleteOwnerListener extends AbstractServiceApiDataFlowListener {

    private static Logger logger = LoggerFactory.getLogger(DeleteOwnerListener.class);

    @Autowired
    private IOwnerBMO ownerBMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Override
    public String getServiceCode() {
        return ServiceCodeConstant.SERVICE_CODE_DELETE_OWNER;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    public void soService(ServiceDataFlowEvent event) {
        logger.debug("ServiceDataFlowEvent : {}", event);

        DataFlowContext dataFlowContext = event.getDataFlowContext();
        AppService service = event.getAppService();

        String paramIn = dataFlowContext.getReqData();

        //校验数据
        validate(paramIn);
        JSONObject paramObj = JSONObject.parseObject(paramIn);

        HttpHeaders header = new HttpHeaders();
        //dataFlowContext.getRequestCurrentHeaders().put(CommonConstant.HTTP_USER_ID, "-1");
        dataFlowContext.getRequestCurrentHeaders().put(CommonConstant.HTTP_ORDER_TYPE_CD, "D");
        JSONArray businesses = new JSONArray();
        businesses.add(ownerBMOImpl.deleteOwner(paramObj));
        if ("1001".equals(paramObj.getString("ownerTypeCd"))) {
            //ownerId 写为 memberId
            paramObj.put("ownerId", paramObj.getString("memberId"));
            RoomDto roomDto = new RoomDto();
            roomDto.setOwnerId(paramObj.getString("ownerId"));
            List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRoomsByOwner(roomDto);
            if(roomDtoList.size() > 0){
                throw new IllegalArgumentException("删除失败,删除前请先解绑房屋信息");
            }
            //查询车位信息
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setOwnerId(paramObj.getString("ownerId"));
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            if(ownerCarDtos.size() > 0){
                throw new IllegalArgumentException("删除失败,删除前请先解绑车位信息");
            }

            //小区楼添加到小区中
            businesses.add(ownerBMOImpl.exitCommunityMember(paramObj));
        }
        ResponseEntity<String> responseEntity = ownerBMOImpl.callService(dataFlowContext, service.getServiceCode(), businesses);

        dataFlowContext.setResponseEntity(responseEntity);
    }
    /**
     * 校验数据
     *
     * @param paramIn 接口请求数据
     */
    private void validate(String paramIn) {
        Assert.jsonObjectHaveKey(paramIn, "memberId", "请求报文中未包含memberId");
        Assert.jsonObjectHaveKey(paramIn, "communityId", "请求报文中未包含communityId");
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    public ICommunityInnerServiceSMO getCommunityInnerServiceSMOImpl() {
        return communityInnerServiceSMOImpl;
    }

    public void setCommunityInnerServiceSMOImpl(ICommunityInnerServiceSMO communityInnerServiceSMOImpl) {
        this.communityInnerServiceSMOImpl = communityInnerServiceSMOImpl;
    }
}
