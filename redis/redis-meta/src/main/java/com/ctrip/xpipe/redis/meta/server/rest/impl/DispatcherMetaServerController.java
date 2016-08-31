package com.ctrip.xpipe.redis.meta.server.rest.impl;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ctrip.xpipe.redis.core.entity.DcMeta;
import com.ctrip.xpipe.redis.core.entity.KeeperContainerMeta;
import com.ctrip.xpipe.redis.core.entity.KeeperInstanceMeta;
import com.ctrip.xpipe.redis.core.entity.KeeperMeta;
import com.ctrip.xpipe.redis.core.entity.KeeperTransMeta;
import com.ctrip.xpipe.redis.core.entity.MetaServerMeta;
import com.ctrip.xpipe.redis.core.meta.ShardStatus;
import com.ctrip.xpipe.redis.core.metaserver.MetaServerKeeperService;
import com.ctrip.xpipe.redis.core.metaserver.MetaServerService;
import com.ctrip.xpipe.redis.meta.server.MetaServer;
import com.ctrip.xpipe.redis.meta.server.cluster.ClusterServers;
import com.ctrip.xpipe.redis.meta.server.cluster.SLOT_STATE;
import com.ctrip.xpipe.redis.meta.server.cluster.SlotInfo;
import com.ctrip.xpipe.redis.meta.server.cluster.SlotManager;
import com.ctrip.xpipe.redis.meta.server.rest.ForwardInfo;
import com.ctrip.xpipe.redis.meta.server.rest.ForwardType;
import com.ctrip.xpipe.redis.meta.server.rest.exception.MovingTargetException;
import com.ctrip.xpipe.redis.meta.server.rest.exception.UnfoundAliveSererException;
import com.ctrip.xpipe.spring.AbstractController;

/**
 * dispatch service to proper server
 * @author wenchao.meng
 *
 * Aug 3, 2016
 */
@RestController
@RequestMapping(MetaServerService.PATH_PREFIX)
public class DispatcherMetaServerController extends AbstractController{
	
	private static final String MODEL_META_SERVER = "MODEL_META_SERVER";
	
	@Autowired
	public MetaServer currentMetaServer;
	
	@Autowired
	private SlotManager slotManager;
	
	@Autowired
	public ClusterServers<MetaServer> servers;
	
	@ModelAttribute
	public void populateModel(@PathVariable final String clusterId, 
			@RequestHeader(name = MetaServerService.HTTP_HEADER_FOWRARD, required = false) ForwardInfo forwardInfo, Model model){

		if(forwardInfo != null){
			logger.info("[ping]{},{}", clusterId, forwardInfo);
		}
		MetaServer metaServer = getMetaServer(clusterId, forwardInfo);
		if(metaServer == null){
			throw new UnfoundAliveSererException(clusterId, slotManager.getServerIdByKey(clusterId), currentMetaServer.getServerId());
		}
		model.addAttribute(MODEL_META_SERVER, metaServer);
		if(forwardInfo != null){
			model.addAttribute(forwardInfo);
		}
	}
	
	
	@RequestMapping(path = MetaServerKeeperService.PATH_SHARD_STATUS, method = RequestMethod.GET, produces= MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ShardStatus getShardStatus(@PathVariable final String clusterId, @PathVariable final String shardId,
			@ModelAttribute ForwardInfo forwardInfo, @ModelAttribute(MODEL_META_SERVER) MetaServer metaServer) throws Exception {
		
		return metaServer.getShardStatus(clusterId, shardId, forwardInfo);
	}


	@RequestMapping(path = MetaServerKeeperService.PATH_PING, method = RequestMethod.POST,  consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void ping(@PathVariable String clusterId, @PathVariable String shardId, KeeperInstanceMeta keeperInstanceMeta,
			@ModelAttribute ForwardInfo forwardInfo, @ModelAttribute MetaServer metaServer) {
		
		metaServer.ping(clusterId, shardId, keeperInstanceMeta, forwardInfo);
	}

	private MetaServer getMetaServer(String clusterId, ForwardInfo forwardInfo) {
		
		int slotId = slotManager.getSlotIdByKey(clusterId);
		SlotInfo slotInfo = slotManager.getSlotInfo(slotId);
		
		if(forwardInfo != null && forwardInfo.getType() == ForwardType.MOVING){

			if(!(slotInfo.getSlotState() == SLOT_STATE.MOVING  && slotInfo.getToServerId() == currentMetaServer.getServerId())){
				throw new MovingTargetException(forwardInfo, currentMetaServer.getServerId(), slotInfo, clusterId, slotId);
			}
			logger.info("[getMetaServer][use current server]");
			return currentMetaServer;
		}
		
		Integer serverId = slotManager.getServerIdByKey(clusterId);
		if(serverId == null){
			throw new IllegalStateException("clusterId:" + clusterId + ", unfound server");
		}
		return servers.getClusterServer(serverId);
	}

	public void addKeeper(String clusterId, String shardId, KeeperMeta keeperMeta) {
		
	}

	public void removeKeeper(String clusterId, String shardId, KeeperMeta keeperMeta) {
		
	}

	public void setKeepers(String clusterId, String shardId, KeeperMeta keeperMeta, List<KeeperMeta> keeperMetas) {
		
	}

	public void clusterChanged(String clusterId) {
		
	}

	public DcMeta getDynamicInfo() {
		return null;
	}

	public List<MetaServerMeta> getAllMetaServers() {
		return null;
	}

	public List<KeeperTransMeta> getAllKeepersByKeeperContainer(KeeperContainerMeta keeperContainerMeta) {
		return null;
	}
	
	public List<KeeperTransMeta> getKeepersByKeeperContainer(KeeperContainerMeta keeperContainerMeta) {
		return null;
	}
}