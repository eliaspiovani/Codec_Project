package com.cloudst.cfs.service;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudst.cfs.annotation.CdnSync;
import com.cloudst.cfs.dao.CdnAccountDao;
import com.cloudst.cfs.dao.ContactDao;
import com.cloudst.cfs.dao.ContactGroupDao;
import com.cloudst.cfs.dao.DlcDocumentDao;
import com.cloudst.cfs.dao.DocLocationDao;
import com.cloudst.cfs.dao.DocumentAccessDao;
import com.cloudst.cfs.dao.DocumentDao;
import com.cloudst.cfs.dao.DocumentLocDeleteDao;
import com.cloudst.cfs.dao.DocumentProfileDao;
import com.cloudst.cfs.dao.SyncTokenDao;
import com.cloudst.cfs.dao.UserDao;
import com.cloudst.cfs.event.DocumentEventListener;
import com.cloudst.cfs.handlres.VideoScaleHandler;
import com.cloudst.cfs.model.CdnAccount;
import com.cloudst.cfs.model.CfsPlanGroupUser;
import com.cloudst.cfs.model.Contact;
import com.cloudst.cfs.model.ContactGroup;
import com.cloudst.cfs.model.CopyEntity;
import com.cloudst.cfs.model.DlcDocument;
import com.cloudst.cfs.model.DlcDocument.ThumbnailType;
import com.cloudst.cfs.model.DocAccessId;
import com.cloudst.cfs.model.DocLocDeleteId;
import com.cloudst.cfs.model.DocLocation;
import com.cloudst.cfs.model.Document;
import com.cloudst.cfs.model.Document.SharingStatus;
import com.cloudst.cfs.model.DocumentAccess;
import com.cloudst.cfs.model.DocumentAccess.Permission;
import com.cloudst.cfs.model.DocumentAccess.Type;
import com.cloudst.cfs.model.DocumentLocationDelete;
import com.cloudst.cfs.model.DocumentProfile;
import com.cloudst.cfs.model.DocumentUserUplaod;
import com.cloudst.cfs.model.File;
import com.cloudst.cfs.model.Folder;
import com.cloudst.cfs.model.Location;
import com.cloudst.cfs.model.Metadata;
import com.cloudst.cfs.model.ShareWithModel;
import com.cloudst.cfs.model.SyncToken;
import com.cloudst.cfs.model.User;
import com.cloudst.cfs.restriction.DocumentRowMapper;
import com.cloudst.cfs.restriction.DownloadCenterRestriction;
import com.cloudst.cfs.restriction.HomeUserRestriction;
import com.cloudst.cfs.util.CfsConstant;
import com.cloudst.cfs.ws.DocumentsChildren;
import com.cloudst.cfs.ws.ShareWith;
import com.cloudst.cfs.ws.ShareWithMe;
import com.cloudst.cfs.ws.SocketManagerService;
import com.framework.core.annotation.Conf;
import com.framework.core.annotation.RefreshCache;
import com.framework.core.annotation.Trace;
import com.framework.core.axon.Axon;
import com.framework.core.axon.AxonBuilder;
import com.framework.core.dao.Dao;
import com.framework.core.dao.Restriction;
import com.framework.core.event.EventListener;
import com.framework.core.exception.ServiceException;
import com.framework.core.mail.MailClient;
import com.framework.core.rest.json.filters.XmlConfigFilter;
import com.framework.core.search.QueryBuilder;
import com.framework.core.search.QueryParser;
import com.framework.core.service.CrudService;
import com.framework.core.service.MessageService;
import com.framework.core.utils.Env;
import com.framework.core.utils.RegexUtil;
import com.framework.core.utils.StringToLongTransformer;
import com.framework.core.utils.StringUtil;
import com.framework.security.model.Group;
import com.framework.security.model.SecurityUser;
import com.framework.security.util.AuthenticationUtil;
import com.framework.websocket.SocketPhase;
import com.framework.websocket.WebSocketParser;
import com.google.common.collect.Lists;

/**
 * @version 0.2
 */
@Service
@Transactional()
public class DocumentService extends CrudService<Document>
{
    
    /**
     *
     private static final long serialVersionUID = -1196878253825388519L;
     */
    
    
    @Autowired
    DocumentDao documentDao;
    
    @Autowired
    DocLocationDao docLocationDao;
    
    @Autowired
    DocumentLocDeleteDao documentLocDeleteDao;
    
    @Autowired
    SyncTokenDao syncTokenDao;
    
    @Conf(configName = "cfs", propertyName = "img.large.height")
    float largeHeight;
    
    @Conf(configName = "cfs", propertyName = "img.large.width")
    float largeWidth;
    
    @Conf(configName = "cfs", propertyName = "img.medium.height")
    float mediumHeight;
    
    @Conf(configName = "cfs", propertyName = "img.medium.width")
    float mediumWidth;
    
    @Conf(configName = "cfs", propertyName = "img.small.width")
    float smallWidth;
    
    @Conf(configName = "cfs", propertyName = "img.small.height")
    int smallHeight;
    
    @Conf(configName = "cfs", propertyName = "location.name")
    protected String currentLocationName;
    
    @Autowired
    UserDao userDao;
    
    @Autowired
    FileSystemRepository fileSystemRepository;
    
    @Autowired
    DocumentAccessService documentAccessService;
    
    @Autowired
    DocumentAccessDao documentAccessDao;
    
    @Autowired
    MailClient mailClient;
    
    @Conf(configName = "mail", propertyName = "from")
    String mailFrom;
    
    @Autowired
    ContactService contactService;
    
    @Autowired
    CfsConstant cfsConstant;
    
    @Autowired
    ContactDao contactDao;
    
    @Autowired
    ContactGroupDao contactGroupDao;
    
    @Autowired
    DocumentsGroupService documentsGroupService;
    
    @Autowired
    EventListener eventListener;
    
    @Autowired
    DocumentProfileDao documentProfileDao;
    
    @Autowired
    DlcDocumentDao dlcDocumentDao;
    
    @Autowired
    UserDlcInvoiceService userDlcInvoiceService;
    
    @Autowired
    DocumentEventListener documentEventListener;
    
    @Autowired
    CdnAccountDao cdnAccountDao;
    
    @Conf(configName = "cfs", propertyName = "emailPath")
    protected String emailPath;
    
    @Autowired
    MessageService messageService;
    
    @Autowired
    CfsPlanGroupUserService cfsPlanGroupUserService;
    
    public void makeFileCensoredByUUID(String uuid){
        documentDao.censoredFileByUuid(uuid);
    }
    /**
     * apply personal records
     *
     * @param ids
     * @return
     * @throws ServiceException
     */
    /* public List<Document> findByIds(List<String> ids, Long userId) throws ServiceException
     {
     /*String query = String.format("select * from fetch_children_of_user(?) where id IN (%s)", QueryParser.buildInWhere(ids));
     return mapToDocuments(getJdbcTemplate().queryForList(query, userId));*/
    //return documentDao.findByIds(userId, "(" + QueryParser.buildInWhere(ids) + "");
    /* List<Long> list = new ArrayList<>();
     for(String string : ids){
     list.add(Long.parseLong(string));
     }
     return documentDao.findByIds(list, getRestriction(), null);
     
     }*/
    public List<Document> findByIds(List<String> ids) throws ServiceException
    {
        return documentDao.findByIds(ids);
    }
    
    public List<Document> findByIds(List<String> ids, Long userId) throws ServiceException
    {
        return documentDao.findByIds(ids, userId);
    }
    
    public List<Document> findTrashByIds(String ids, Long userId)
    {
        String query = String.format("select * from fetch_children_of_user_trash(?) where id IN (%s)", QueryParser.buildInWhere(ids));
        return mapToDocuments(getJdbcTemplate().queryForList(query, userId));
        
    }
    
    public List<Document> findShareByIds(List<String> ids, Long userId)
    {
        String query = String.format("select * from fetch_children_of_user_share(?) where id IN (%s)", QueryParser.buildInWhere(ids));
        return mapToDocuments(getJdbcTemplate().queryForList(query, userId));
    }
    
    public boolean isUserDocumentShared(Long userId, Long docId){
        /* String query = String.format("select * from fetch_children_of_user_share(?) ", userId, docId);
         ResultSetExtractor< Boolean > rse =
         return getJdbcTemplate().query(query, new ResultSetExtractor<Boolean>()));*/
        return false;
    }
    public List<Document> mapToDocuments(List<Map<String, Object>> rows)
    {
        List<Document> docs = new ArrayList<Document>();
        for (Map<String, Object> row : rows)
        {
            Object owner = row.get("owner_id");
            if (owner == null)
                owner = row.get("user_id");
            try
            {
                User u = new User();
                u.setId((Long) owner);
                String dn = (String) row.get("display_name");
                u.setDisplayName(dn == null ? (String) row.get("created_by") : dn);
                u.setEmail((String) row.get("email"));
                Object pid = row.get("parent_id");
                Document parent = null;
                if (pid != null)
                {
                    parent = new Document((Long) pid);
                    parent.setFullPath((String) row.get("parentpath"));
                    parent.setUuid((String) row.get("parent_uuid"));
                }
                Type type = Type.valueOf((String) row.get("type"));
                boolean preview = row.get("preview") != null ? (Boolean) row.get("preview") : false;
                long dlCnt = row.get("download_count") == null ? 0L : (long) row.get("download_count");
                Document d = new Document((long) row.get("id"), (String) row.get("document_type"), u, parent, type,
                                          (Boolean) row.get("favourite"), StringUtil.nullable(row.get("fullpath")),
                                          StringUtil.nullable(row.get("parentpath")), StringUtil.nullable(row.get("treepath")), null, preview, dlCnt,
                                          (String) row.get("downloaded_ips"), (String) row.get("properties"), (String) row.get("name"),
                                          (String) row.get("discriminator"), (String) row.get("uuid"), (Long) row.get("size"),
                                          (String) row.get("created_by"), (Date) row.get("updated_at"), (Date) row.get("created_at"),
                                          (String) row.get("extension"), (String) row.get("mime_type"), (Boolean) row.get("shared"));
                d.setSharingStatus(Document.SharingStatus.getSharingStatus((String) row.get("sharing_status")));
                d.setDescription((String) row.get("description"));
                d.setBoost((BigDecimal) row.get("boost"));
                Object dpId = row.get("doc_profile_id");
                
                if (dpId != null)
                {
                    //Long uid = null;
                    DocumentProfile profile = documentProfileDao.find((Long) dpId);
                    
                    d.setDocumentProfile(profile);
                }
                docs.add(d);
            }
            catch (Exception e)
            {
                logger.info("DocumentService mapToDocuments failed");
            }
            catch (ServiceException e)
            {
                logger.info(e.getMessage());
            }
        }
        return docs;
    }
    
    public List<Document> mapShareToDocuments(List<Map<String, Object>> rows)
    {
        List<Document> docs = new ArrayList<Document>();
        for (Map<String, Object> row : rows)
        {
            Object owner = row.get("owner_id");
            if (owner == null)
                owner = row.get("user_id");
            try
            {
                User u = new User();
                u.setId((Long) row.get("uid"));
                String dn = (String) row.get("display_name");
                u.setDisplayName(dn == null ? (String) row.get("created_by") : dn);
                u.setEmail((String) row.get("email"));
                u.setFreeUser((Boolean) row.get("free_user"));
                Object pid = row.get("parent_id");
                Document parent = null;
                if (pid != null)
                {
                    parent = new Document((Long) pid);
                    parent.setFullPath((String) row.get("parentpath"));
                    parent.setUuid((String) row.get("parent_uuid"));
                }
                Type type = Type.valueOf((String) row.get("type"));
                Document d = new Document((long) row.get("id"), (String) row.get("document_type"), u, parent, type,
                                          (Boolean) row.get("favourite"), (String) row.get("fullpath"), (String) row.get("parentpath"),
                                          (String) row.get("treepath"), null, (Boolean) row.get("preview"), (Long) row.get("download_count"),
                                          (String) row.get("downloaded_ips"), (String) row.get("properties"), (String) row.get("name"),
                                          (String) row.get("discriminator"), (String) row.get("uuid"), (Long) row.get("size"),
                                          (String) row.get("created_by"), (Date) row.get("updated_at"), (Date) row.get("created_at"),
                                          (String) row.get("extension"), (String) row.get("mime_type"), (Boolean) row.get("shared"));
                d.setDescription((String) row.get("description"));
                Object dpId = row.get("doc_profile_id");
                if (dpId != null)
                {
                    DocumentProfile profile = documentProfileDao.find((Long) dpId);
                    d.setDocumentProfile(profile);
                }
                docs.add(d);
            }
            catch (Exception e)
            {
                logger.info("DocumentService mapShareToDocuments failed");
            }
            catch (ServiceException e)
            {
                logger.info(e.getMessage());
                
            }
        }
        return docs;
    }
    
    public List<Document> findByUuids(List<String> uuids)
    {
        return documentDao.findByUuids(uuids);
    }
    
    @Override
    public Dao<Document> getDao()
    {
        return documentDao;
    }
    
    @Override
    public Restriction getRestriction()
    {
        
        return new HomeUserRestriction(AuthenticationUtil.getUserId());
    }
    
    @Override
    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
    public void delete(Document entity) throws ServiceException
    {
        User owner = entity.getOwner();
        deleteDoc(String.valueOf(entity.getId()), owner.getId());
        owner.setQuota(documentDao.callUsedSpace(owner.getId()));
        userDao.update(owner);
    }
    
    private void deleteDoc(Document document)
    {
        List<Long> deletedIds = new ArrayList<Long>();
        if (document.getDiscriminator().equals(Folder.DISCRIMINATOR_VALUE)) {
            String path = document.getCfsTreePath().concat("%");
            List<Document> deletedFiles = documentDao.findDeletedFolderChildren(path, document.getOwner().getId());
            for (Document doc : deletedFiles) {
                deletedIds.add(doc.getId());
            }
        } else {
            deletedIds.add(document.getId());
        }
        
        new Thread(new deleteFilesPhysically(deletedIds)).start();
    }
    
    private void recursiveDelete(Document document) throws ServiceException
    {
        String path = document.getCfsTreePath().concat("%");
        List<Document> deletedFiles = documentDao.findDeletedFolderChildren(path, document.getOwner().getId());
        
        List<DocLocation> locations = docLocationDao.findDocLocationByDocId(document.getId());
        for(DocLocation location : locations)
        {
            if(DocLocation.TransferStatus.SUCCESS.equals(location.getTransferStatus()))
            {
                DocLocDeleteId deleteId = new DocLocDeleteId(location.getLocation().getId(), document.getId());
                DocumentLocationDelete  locationDelete = new DocumentLocationDelete();
                locationDelete.setDocLocDeleteId(deleteId);
                locationDelete.setDocument(document.getId());
                locationDelete.setLocation(location.getLocation());
                documentLocDeleteDao.save(locationDelete);
            }
        }
    }
    
    private void deleteDoc(String commaSeparatedIds, Long ownerId) throws ServiceException
    {
        List<Long> transformIds = Lists.transform(StringUtil.split(commaSeparatedIds, ","), new StringToLongTransformer());
        String ids = "";
        for (Long id : transformIds)
        {
            ids = ids + "," + recursiveDelete(id);
        }
        List<Long> deletedIds = Lists.transform(StringUtil.split(ids, ","), new StringToLongTransformer());
        new Thread(new deleteFilesPhysically(deletedIds)).start();
    }
    
    @Transactional
    private String recursiveDelete(Long docId) throws ServiceException
    {
        String ids = "";
        List<Document> documents = documentDao.findDeletedByParentId(docId);
        for (Document doc : documents)
        {
            ids = ids + "," + recursiveDelete(doc.getId());
        }
        List<DocLocation> locations = docLocationDao.findDocOtherSuccessLocation(docId, currentLocationName);
        for(DocLocation location : locations)
        {
            if(DocLocation.TransferStatus.SUCCESS.equals(location.getTransferStatus()))
            {
                DocLocDeleteId deleteId = new DocLocDeleteId(location.getLocation().getId(), docId);
                DocumentLocationDelete  locationDelete = new DocumentLocationDelete();
                locationDelete.setDocLocDeleteId(deleteId);
                locationDelete.setDocument(docId);
                locationDelete.setLocation(location.getLocation());
                documentLocDeleteDao.save(locationDelete);
            }
        }
        try
        {
            documentDao.remove(documentDao.find(docId, null));
        }
        catch (ServiceException e)
        {
            throw new ServiceException(getMessage("deleteIsImpossible", null));
        }
        catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        ids = ids+","+docId;
        return ids;
    }
    
    @Override
    @Trace(enable = false)
    protected void beforeInsert(Document entity) throws ServiceException
    {
        if (entity.getDiscriminator() == null)
        {
            entity.setDiscriminator(Folder.DISCRIMINATOR_VALUE);
        }
        if (!entity.getDiscriminator().equals(Folder.DISCRIMINATOR_VALUE))
        {
            throw new ServiceException(getMessage("onlyFolderOrFile", null));
            
        }
        
        User currentUser = userDao.find(AuthenticationUtil.getUserId());
        Document parent = retrieveParent(entity);
        entity.setParent(parent);
        entity.setCfsFullPath((entity.getParent() != null && entity.getParent().getCfsFullPath() != null) ? entity.getParent().getCfsFullPath().concat("/").concat(entity.getName()) : "/".concat(entity.getName()));
        entity.setOwner(currentUser);
        if (entity.getParent() == null)
        {
            //User currentUser = userDao.findByUsername(AuthenticationUtil.getUsername());
            entity.setSharingStatus(currentUser.getCfsRootSharingStatus());
        }
        // also see documentVO#prepersist mehtod
        entity.setOwner(currentUser);
        if(entity.getParent() != null && entity.getParent().isShared())
            entity.setShared(true);
        StringEscapeUtils.escapeXml(entity.getDescription());
        StringEscapeUtils.escapeHtml4(entity.getDescription());
    }
    
    public Document retrieveParent(Document entity) throws ServiceException
    {
        Document parent = null;
        // parse json parent
        if (entity.getParent() == null || entity.getParent().getId() == null)
        {
            parent = null;
        }
        else
        {
            Restriction parentRestriction = null;
            if (entity.isDlc())
                parentRestriction = new DownloadCenterRestriction(AuthenticationUtil.getUserId());
            else
                parentRestriction = new HomeUserRestriction(AuthenticationUtil.getUserId());
            Document doc = documentDao.find(entity.getParent().getId(), parentRestriction);
            if (doc == null)
            {
                throw new ServiceException(getMessage("parentNotExist", null));
            }
            if (doc.isDeleted())
            {
                throw new ServiceException(getMessage("notUploadToTrash", null));
            }
            parent = doc;
        }
        return parent;
    }
    
    @Override
    @Trace(enable = false)
    protected void afterInsert(Document entity, boolean success) throws ServiceException
    {
        if (success)
        {
            entity.setCfsTreePath((entity.getParent() != null && entity.getParent().getCfsTreePath() != null) ? entity.getParent().getCfsTreePath().concat("/").concat(entity.getId().toString()) : (entity.getId().toString()));
            update(entity);
        }
    }
    
    @Override
    @Trace(enable = false)
    protected void beforeUpdate(Document jsonEntity, Document entity, Document oldEntity) throws ServiceException
    {
        //todo: we should decide about favorite
        /*if (jsonEntity.getFavourite() != null)
         {
         if (jsonEntity.getFavourite().booleanValue() != oldEntity.getFavourite().booleanValue())
         {
         if (!entity.getDiscriminator().equals(Folder.DISCRIMINATOR_VALUE))
         {
         throw new ServiceException(getMessage("favouriteFolder", null));
         }
         documentAccessService.updateFavourite(entity);
         }
         }*/
        if (StringUtil.isNotEmpty(jsonEntity.getName()))
        {
            throw new UnsupportedOperationException(getMessage("deprecatedService", null));
            // if (documentDao.DocExists(jsonEntity.getName(),
            // AuthenticationUtil.getUsername(), oldEntity.getParent().getId(),
            // oldEntity.getDiscriminator()) != null)
            // {
            // throw new ServiceException("Name already exists");
            // }
        }
        StringEscapeUtils.escapeXml(entity.getDescription());
        StringEscapeUtils.escapeHtml4(entity.getDescription());
        
    }
    
    @Transactional
    public void batchUpdate(List<Document> entityList, Document jsonEntity, List<Long> idList)
    {
        
        if (isBatchFavourite(jsonEntity))
        {
            documentAccessService.batchUpdateFavourite(jsonEntity, idList);
        }
        // FIXME
        List<Document> docUpdatedList = null;// findByIds(idList);
        entityList.clear();
        entityList.addAll(docUpdatedList);
    }
    
    private boolean isBatchFavourite(Document jsonEntity)
    {
        //todo: we should decide about favorite
        return false;// jsonEntity.getFavourite() != null;
    }
    
    @Transactional()
    @RefreshCache
    public boolean move(Long targetId, String ids, Long uid) throws ServiceException
    {
        Document target = documentDao.find(targetId, getRestriction());
        if(targetId != null && !targetId.equals(1L) && target == null)
        {
            throw new ServiceException(getMessage("notMoveAllowed", null));
        }
        if (target != null)
        {
            if (target.isDeleted())
            {
                throw new ServiceException(getMessage("notMoveToTrash", null));
            }
            setInnerDocumentProperties(target);
        }
        String[] srcIdList = ids.split(",");
        
        for (String id : srcIdList)
        {
            if(id.equals(targetId.toString()))
                throw new ServiceException(getMessage("notMoveAllowed", null));
            
        }
        
        for (String id : srcIdList)
        {
            
            Document srcDocument = documentDao.find(Long.valueOf(id), getRestriction());
            if(srcDocument == null)
                throw new ServiceException("user is not owner");
            setInnerDocumentProperties(srcDocument);
            
            if(target != null && target.getCfsTreePath().startsWith(srcDocument.getCfsTreePath()))
                throw new ServiceException(getMessage("notMoveAllowed", null));
            
            if(target == null && srcDocument.getParent() == null)
                throw new ServiceException(getMessage("notMoveAllowed", null));
            
            if(target != null && srcDocument.getParent() != null && srcDocument.getParent().getId().equals(target.getId()))
                throw new ServiceException(getMessage("notMoveAllowed", null));
            
            /* if (srcDocument.getSharingStatus() == null)
             {
             User currentUser = userDao.findByUsername(AuthenticationUtil.getUsername());
             srcDocument.setSharingStatus(currentUser.getCfsRootSharingStatus());
             }*/
            
            List<Document> documents = null;
            if(target == null)
                documents = documentDao.findSimilarDocInRoot(srcDocument.getName(), uid);
            else
                documents = documentDao.findSimilarDocInTarget(targetId, srcDocument.getName(), uid);
            
            if(!documents.isEmpty()){
                
                if (documents.get(0).getDiscriminator().equals(com.cloudst.cfs.model.File.DISCRIMINATOR_VALUE))
                {
                    throw new ServiceException(getMessage("fileExist", null));
                }
                else
                {
                    throw new ServiceException(getMessage("folderExist", null));
                }
            }
            
            if(srcDocument.isFolder()){
                
                String oldPath = srcDocument.getCfsFullPath();
                String newPath = (target != null && target.getCfsFullPath() != null) ? target.getCfsFullPath().concat("/").concat(srcDocument.getName()) : ("/").concat(srcDocument.getName()) ;
                
                String oldTreePath = srcDocument.getCfsTreePath();
                String newTreePath = ((target != null && target.getCfsTreePath() != null) ? target.getCfsTreePath().concat("/").concat(srcDocument.getId().toString()) : (srcDocument.getId().toString()));
                
                srcDocument.setCfsFullPath(newPath);
                srcDocument.setCfsTreePath((target != null && target.getCfsTreePath() != null) ? target.getCfsTreePath().concat("/").concat(srcDocument.getId().toString()) : (srcDocument.getId().toString()));
                
                srcDocument.setParent(target);
                update(srcDocument);
                
                documentDao.updateFullPath(oldPath, newPath, AuthenticationUtil.getUserId());
                documentDao.updateTreePath(oldTreePath, newTreePath, AuthenticationUtil.getUserId());
            }else{
                srcDocument.setParent(target);
                srcDocument.setCfsFullPath((target != null && target.getCfsFullPath() != null) ? target.getCfsFullPath().concat("/").concat(srcDocument.getName()) : "/".concat(srcDocument.getName()));
                srcDocument.setCfsTreePath((target != null && target.getCfsTreePath() != null) ? target.getCfsTreePath().concat("/").concat(srcDocument.getId().toString()) : (srcDocument.getId().toString()));
                update(srcDocument);
            }
            
            //documentEventListener.publish(srcDocument);
        }
        return true;
    }
    
    public void sendShareEmail(List<Document> docs, String userEmail, SecurityUser currentUser, boolean sendForMembers)
    {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("user", currentUser);
        model.put("interfaceUrl", cfsConstant.getInterfaceUrl(null));
        List<String> ids = new ArrayList<String>();
        for (Document document : docs)
        {
            ids.add(String.valueOf(document.getId()));
        }
        
        model.put("emailP", userEmail);
        String membertemplate = null;
        String nonMembertemplate = null;
        String subject = null;
        
        if(currentUser.getLanguage() != null)
        {
            
            membertemplate = emailPath.concat("memberShareLink").concat("_").concat(currentUser.getLanguage().getCode()).concat(".vm");
            nonMembertemplate = emailPath.concat("shareLink").concat("_").concat(currentUser.getLanguage().getCode()).concat(".vm");
            subject = currentUser.getDisplayName() + messageService.getMessage(currentUser.getLanguage(), "shareFile", null);
        }
        else
        {
            membertemplate = emailPath.concat("memberShareLink_fa.vm");
            nonMembertemplate = emailPath.concat("shareLink_fa.vm");
            subject = currentUser.getDisplayName()
            + " \u0641\u0627\u06CC\u0644 \u06CC\u0627 \u0641\u0648\u0644\u062F\u0631\u06CC \u0631\u0627 \u0628\u0627 \u0634\u0645\u0627 \u0628\u0647 \u0627\u0634\u062A\u0631\u0627\u06A9 \u06AF\u0630\u0627\u0634\u062A\u0647 \u0627\u0633\u062A";
        }
        mailClient.send(mailFrom, userEmail, subject, sendForMembers ? membertemplate : nonMembertemplate, model, null);
    }
    
    public void batchDelete(List<Document> entityList, List<Long> idList, String ids) throws ServiceException
    {
        Long uid = AuthenticationUtil.getUserId();
        deleteDoc(ids, uid);
        documentAccessDao.updateUserSpace(uid);
    }
    
    public List<CopyEntity> copy(String ids, User user, Long targetId) throws ServiceException
    {
        // accumalation copy files
        long volume = documentAccessDao.volume(ids, user.getId());
        long planQuota = cfsConstant.getUserPlanQuota(user);
        if (planQuota < (volume + user.getQuota()))
        {
            throw new ServiceException(getMessage("notFreeSpace", null));
        }
        //
        List<CopyEntity> copyEntityList = documentAccessDao.copy(ids, targetId, user.getId());
        // update user quota
        userDao.increaseQuota(volume, user.getUsername());
        return copyEntityList;
    }
    
    public void businessGroupCheck(User currentUser, List<String> givenEmailsOrGroupName) throws ServiceException
    {
        Set<String> set = new HashSet<String>();
        
        for(Group group : currentUser.getGroups())
            set.add(group.getName());
        
        if(set.contains(User.ADMIN_CFS_GROUP) || set.contains(User.USER_CFS_GROUP))
        {
            cfsPlanGroupUserService.userGroupSharingActive(currentUser.getId(), givenEmailsOrGroupName);
        }
    }
    
    @Transactional
    @RefreshCache
    public ShareWithModel share(String documentIds, List<String> givenEmailsOrGroupName, SecurityUser currentUser) throws ServiceException
    {
        
        User user = userDao.find(currentUser.getId());
        
        businessGroupCheck(user, givenEmailsOrGroupName);
        
        List<String> emails = new ArrayList<String>(givenEmailsOrGroupName);
        List<String> groups = new ArrayList<String>(givenEmailsOrGroupName);
        CollectionUtils.filter(emails, new org.apache.commons.collections.Predicate()
                               {
            
            @Override
            public boolean evaluate(Object object)
            {
                String email = (String) object;
                if (RegexUtil.match(RegexUtil.EMAIL_PATTERN, email))
                    return true;
                else
                    return false;
            }
        });
        //
        CollectionUtils.filter(groups, new org.apache.commons.collections.Predicate()
                               {
            
            @Override
            public boolean evaluate(Object object)
            {
                String email = (String) object;
                if (RegexUtil.match(RegexUtil.EMAIL_PATTERN, email))
                    return false;
                else
                    return true;
            }
        });
        List<ContactGroup> contactGroups = new ArrayList<ContactGroup>();
        List<User> users = new ArrayList<User>();
        if (CollectionUtils.isNotEmpty(groups))
        {
            contactGroups = processGroups(groups, documentIds, currentUser);
        }
        if (CollectionUtils.isNotEmpty(emails))
        {
            users = processEmails(emails, documentIds, currentUser);
        }
        List<String> ids = StringUtil.split(documentIds, ",");
        
        List<Document> docs = documentDao.findByIds(ids);
        if(docs.size() > 0 && users.size()>0)
        {
            Axon axon_user = new AxonBuilder().addFilter(new XmlConfigFilter(User.class, "default")).create();
            Axon axon_doc = new AxonBuilder().addFilter(new XmlConfigFilter(Document.class, "default")).create();
            String docoutput = "[" + axon_doc.toJson(docs) + "]";
            String userOutput = "[" + axon_user.toJson(users) + "]";
            
            String childrenExpr = WebSocketParser.buildExpression(DocumentsChildren.class, WebSocketParser.OBJECT_ID, docoutput,
                                                                  SocketPhase.UPDATED);
            String expr = WebSocketParser.buildExpression(ShareWith.class, WebSocketParser.OBJECT_ID, userOutput, SocketPhase.INSERTED);
            eventListener.raiseEvent(SocketManagerService.FIRE_EVENT_NAME_WITH_STRING, childrenExpr, AuthenticationUtil.getUsername());
            eventListener.raiseEvent(SocketManagerService.FIRE_EVENT_NAME_WITH_STRING, expr, AuthenticationUtil.getUsername());
        }
        ShareWithModel swm = new ShareWithModel(docs, users, contactGroups);
        
        return swm;
    }
    
    @Transactional
    private List<User> processEmails(List<String> emails, String documentIds, SecurityUser currentUser) throws ServiceException
    {
        List<String> usersEmail = new ArrayList<String>();
        List<User> users = userDao.findAllByEmails(emails);
        List<String> ids = StringUtil.split(documentIds, ",");
        List<String> userIds = new ArrayList<String>();
        List<Document> docs = documentDao.findByIds(ids, AuthenticationUtil.getUserId());
        if (CollectionUtils.isNotEmpty(users))
        {
            for (User user : users)
            {
                List<Document> newShareDocs = new ArrayList<>();
                for(Document document : docs)
                {
                    List<DocumentAccess> deniedAccesses = documentAccessDao.findDeniedByDocIdUserId(document.getId(), user.getId());
                    
                    if(!documentDao.docSharedWithUser(user.getId(), document.getId()) && deniedAccesses.size() == 0)
                    {
                        newShareDocs.add(document);
                        
                    }
                    if(deniedAccesses.size()>0)
                    {
                        for(DocumentAccess access: deniedAccesses)
                        {
                            access.setPermission(Permission.VIEW);
                            documentAccessService.update(access);
                            
                            if(!document.isShared()){
                                if(document.getDiscriminator().equals(File.DISCRIMINATOR_VALUE)){
                                    document.setShared(true);
                                    update(document);
                                }else{
                                    documentDao.updateSharedDocChildWithPath(document.getId(), user.getId());
                                }
                            }
                            
                        }
                    }
                }
                usersEmail.add(user.getEmail());
                userIds.add(user.getId().toString());
                // share with registered users and send email
                documentAccessService.saveShare(newShareDocs, user);
                if(newShareDocs.size() > 0)
                {
                    sendShareEmail(newShareDocs, user.getEmail(), currentUser, true);
                    StringBuffer documentsId = new StringBuffer();
                    for(Document document : newShareDocs){
                        documentsId.append(document.getId());
                        documentsId.append(",");
                    }
                    if(documentsId.length() > 1)
                    {
                        processShareWebsocket(user, documentsId.toString().substring(0, documentsId.toString().length()-1));
                    }
                }
            }
            //            for(Document document : docs)
            //            {
            //                if(!document.isShared())
            //                {
            //                    document.setShared(true);
            //                    documentDao.update(document);
            //                    if(Folder.DISCRIMINATOR_VALUE.equals(document.getDiscriminator()))
            //                    {
            //                        String path = document.getCfsFullPath().concat("/%");
            //                        List<DocumentAccess> subFolderDenied = documentAccessDao.fetchUserSubfolderDenied(currentUser.getId(), path);
            //                         Collections.sort(subFolderDenied, new Comparator<DocumentAccess>() {
            //                             @Override
            //                            public int compare(DocumentAccess o1, DocumentAccess o2) {
            //                                 Integer o1c = o1.getDocument().getCfsFullPath().split("/").length;
            //                                 Integer o2c = o2.getDocument().getCfsFullPath().split("/").length;
            //                                return o2c.compareTo(o1c);
            //                            }
            //                        });
            //
            //                         documentDao.updateDocumentShared(path, currentUser.getId());
            //
            //                        Map<Document, List<User>> tempMap = new HashMap<Document, List<User>>();
            //                        List<Document> denied = new ArrayList<Document>();
            //
            //                        for(DocumentAccess access : subFolderDenied)
            //                        {
            //                            if(tempMap.containsKey(access.getDocument()))
            //                                tempMap.get(access.getDocument()).add(access.getUser());
            //                            else
            //                            {
            //                                for(Map.Entry<Document, List<User>> entry : tempMap.entrySet())
            //                                {
            //                                    Set<User> usersTemp = new HashSet<User>();
            //                                    usersTemp.addAll(users);
            //                                    for(User u : entry.getValue())
            //                                    {
            //                                        if(usersTemp.contains(u))
            //                                            usersTemp.remove(u);
            //                                    }
            //                                    if(usersTemp.isEmpty())
            //                                        denied.add(entry.getKey());
            //                                }
            //                                for(Document doc :denied)
            //                                {
            //                                    if(File.DISCRIMINATOR_VALUE.equals(doc.getDiscriminator()))
            //                                    {
            //                                        document.setShared(false);
            //                                        documentDao.update(doc);
            //                                    }
            //                                    else
            //                                    {
            //                                         documentDao.updateDocumentNotShared(path, currentUser.getId());
            //
            //                                    }
            //                                }
            //                                tempMap = new HashMap<Document, List<User>>();
            //                                List<User> tmp = new ArrayList<User>();
            //                                tmp.add(access.getUser());
            //                                tempMap.put(access.getDocument(), tmp);
            //                            }
            //                        }
            //                    }
            //                }
            //            }
            
        }
        Collection<String> nonRegisterUsers = CollectionUtils.subtract(emails, usersEmail);
        if (CollectionUtils.isNotEmpty(nonRegisterUsers))
        {
            for (String emailContact : nonRegisterUsers)
            {
                sendShareEmail(docs, emailContact, currentUser, false);
            }
        }
        addToContacts(emails);
        return users;
    }
    
    
    private List<ContactGroup> processGroups(List<String> groups, String documentIds, SecurityUser currentUser) throws ServiceException
    {
        List<String> userEmails = new ArrayList<String>();
        List<ContactGroup> contactGroups = contactGroupDao.findByGroupsNames(groups, AuthenticationUtil.getUserId());
        List<Contact> contactList = contactDao.findByGroupName(groups, currentUser.getId());
        List<String> ids = StringUtil.split(documentIds, ",");
        List<Document> docs = documentDao.findByIds(ids, AuthenticationUtil.getUserId());
        for (Contact contact : contactList)
        {
            userEmails.add(contact.getEmail());
            if (contact.getUser() != null)
            {
                // contact is registered already
                //processShareWebsocket(contact.getUser(), documentIds);
                
            }
        }
        
        if (CollectionUtils.isNotEmpty(userEmails))
            processEmails(userEmails, documentIds, currentUser);
        if (CollectionUtils.isNotEmpty(contactGroups))
            assignDocToGroup(contactGroups, docs);
        return contactGroups;
    }
    
    
    private void processShareWebsocket(User user, String documentIds)
    {
        String sharedExpr = WebSocketParser.buildExpression(ShareWithMe.class, WebSocketParser.ARRAY_ID, documentIds, SocketPhase.INSERTED);
        String expr = WebSocketParser.buildExpression(ShareWith.class, WebSocketParser.ARRAY_ID, documentIds, SocketPhase.INSERTED);
        eventListener.raiseEvent(SocketManagerService.FIRE_EVENT_NAME_WITH_STRING, sharedExpr, user.getUsername());
        eventListener.raiseEvent(SocketManagerService.FIRE_EVENT_NAME_WITH_STRING, expr, user.getUsername());
    }
    
    @Transactional
    private void assignDocToGroup(List<ContactGroup> contactGroups, List<Document> docs) throws ServiceException
    {
        if (CollectionUtils.isNotEmpty(contactGroups))
        {
            for (ContactGroup contactGroup : contactGroups)
            {
                documentsGroupService.saveShare(docs, contactGroup);
                
            }
        }
    }
    
    @Transactional
    private void addToContacts(List<String> emails) throws ServiceException
    {
        User u = User.makeUserRef();
        List<Contact> userContacts = contactService.findAllByEmails(u, emails);
        for (Contact contact : userContacts)
        {
            emails.remove(contact.getEmail());
        }
        for (String newEmail : emails)
        {
            User user = userDao.findByEmail(newEmail);
            Contact contact = new Contact(user, newEmail);
            contactService.insert(contact);
        }
    }
    
    @Transactional
    public void customThumbnail(Document document, String entityPreviewLocation, String docPreviewLocation, Document entity, int x, int y,
                                final int regionWidth, final int regionHeight) throws ServiceException
    {
        try
        {
            if (document.isPreview())
            {
                if (document.isGenericImage())
                {
                    java.io.File f = new java.io.File(docPreviewLocation);
                    java.io.File dest = new java.io.File(entityPreviewLocation);
                    FileUtils.deleteDirectory(dest);
                    FileUtils.copyDirectory(f, dest, true);
                    Collection<java.io.File> listFiles = FileUtils.listFiles(dest, null, false);
                    for (java.io.File file : listFiles)
                    {
                        String fname = file.getName();
                        String newName = fname.substring(0, fname.indexOf("_")) + "_" + entity.getId();
                        final java.io.File newFile = new java.io.File(entityPreviewLocation + Env.SEPARATOR + newName);
                        //OutputStream ou = new FileOutputStream(newFile);
                        FileUtils.moveFile(file, newFile);
                        //                        HashMap<String, Float> widthHeight = ImageUtil.calculateHeightWithBaseMediumSize(x, y, regionWidth, regionHeight, fname);
                        //                        Coordinate cor = new Coordinate(widthHeight.get("x").intValue(), widthHeight.get("y").intValue());
                        //
                        //                        ImageUtil.thumbnailsOfWithCropCoordinate(file,
                        //                          widthHeight.get("width").intValue(),
                        //                          widthHeight.get("height").intValue(),
                        //                          ou, cor);
                        
                        file.delete();
                    }
                }
                else if (document.isGenericVideo())
                {
                    VideoScaleHandler vsc = new VideoScaleHandler();
                    String properties = document.getProperties();
                    if (StringUtil.isNotEmpty(properties))
                    {
                        JSONObject json = new JSONObject(properties);
                        JSONArray streams = json.getJSONArray("streams");
                        int height = 0;
                        int width = 0;
                        for (int i = 0; i < streams.length(); i++)
                        {
                            JSONObject item = streams.getJSONObject(i);
                            if (item.has("Height") && item.has("Width"))
                            {
                                height = item.getInt("Height");
                                width = item.getInt("Width");
                                break;
                            }
                        }
                        
                        java.io.File dest = new java.io.File(entityPreviewLocation);
                        FileUtils.deleteDirectory(dest);
                        dest.mkdirs();
                        String docLocation = fileSystemRepository.getDocumentLocation(document) + Env.SEPARATOR + document.getId();
                        vsc.scale(docLocation, width, height, dest.getAbsolutePath(), entity.getId());
                    }
                }
                else
                {
                    throw new ServiceException(getMessage("noPreview",null));
                }
                DlcDocument dlcDocument = entity.getDlcDocument();
                if(dlcDocument == null)
                {
                    dlcDocument = new DlcDocument();
                    dlcDocument.setThumbnailType(document.isGenericImage() ? ThumbnailType.IMAGE : ThumbnailType.VIDEO);
                    dlcDocument.setDocument(entity);
                    dlcDocumentDao.insert(dlcDocument);
                }
                else
                {
                    dlcDocument.setThumbnailType(document.isGenericImage() ? ThumbnailType.IMAGE : ThumbnailType.VIDEO);
                    dlcDocumentDao.update(dlcDocument);
                }
            }
            else
            {
                throw new ServiceException(getMessage("noPreview",null));
            }
            
        }
        
        catch (IOException e)
        {
            throw new ServiceException(e.getMessage());
        }
        catch (Exception ex)
        {
            throw new ServiceException(ex.getMessage());
        }
        
    }
    
    public void scaleVideo(Document entity, String docPreviewLocation,
                           int width, int height) throws ServiceException
    {
        try
        {
            if (entity.isPreview() || entity.isGenericVideo())
            {
                VideoScaleHandler vsc = new VideoScaleHandler();
                String properties = entity.getProperties();
                if (StringUtil.isNotEmpty(properties))
                {
                    java.io.File dest = new java.io.File(docPreviewLocation);
                    if (!dest.exists())
                        dest.mkdirs();
                    String docLocation = fileSystemRepository.getDocumentLocation(entity) + Env.SEPARATOR + entity.getId();
                    vsc.scale(docLocation, width, height, dest.getAbsolutePath(), entity.getId());
                }
                //                DocumentProfile documentProfile = entity.getDocumentProfile();
                //                if (documentProfile == null)
                //                {
                //                    documentProfile = new DocumentProfile();
                //                    documentProfile.setThumbnailType(ThumbnailType.video);
                //                    documentProfile.setDocument(entity);
                //                    documentProfileDao.insert(documentProfile);
                //                }
                //                else
                //                {
                //                    documentProfile.setThumbnailType(ThumbnailType.video);
                //                    documentProfileDao.update(documentProfile);
                //                }
            }
            else
            {
                throw new ServiceException(getMessage("noPreview",null));
            }
            
        }
        
        catch (Exception ex)
        {
            throw new ServiceException(ex.getMessage());
        }
        
    }
    
    
    public Document moveTo(Document entity, Document docTo, boolean before) throws ServiceException
    {
        Document document = null;
        StringBuffer sb = new StringBuffer("select * from docs d where d.user_id = ? ");
        Object[] args = null;
        if (docTo.getParent() == null)
        {
            args = new Object[2];
            sb.append(" AND d.parent_id is null ");
            args[1] = docTo.getBoost();
        }
        else
        {
            args = new Object[3];
            sb.append(" AND d.parent_id = ?");
            args[1] = docTo.getParent().getId();
            args[2] = docTo.getBoost();
        }
        args[0] = AuthenticationUtil.getUserId();
        if (before)
        {
            sb.append(" AND d.boost < ?  order by d.boost DESC limit 1 ");
            try
            {
                document = getJdbcTemplate().queryForObject(sb.toString(), new DocumentRowMapper(), args);
            }
            catch (DataAccessException e)
            {
            }
            
        }
        else
        {
            sb.append(" AND d.boost > ?  order by d.boost ASC limit 1 ");
            try
            {
                document = getJdbcTemplate().queryForObject(sb.toString(), new DocumentRowMapper(), args);
            }
            catch (DataAccessException e)
            {
                // DO NOTHING
            }
        }
        BigDecimal newBoost = docTo.getBoost();
        if (document == null)
        {
            if (before)
                newBoost = docTo.getBoost().subtract(BigDecimal.ONE);
            else
                newBoost = docTo.getBoost().add(BigDecimal.ONE);
        }
        else
        {
            newBoost = docTo.getBoost().add(document.getBoost()).divide(new BigDecimal(2));
        }
        
        entity.setBoost(newBoost);
        update(entity);
        return entity;
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<DocumentUserUplaod> getUserDocuments(Date dateFrom, Date dateTo)
    {
        List<DocumentUserUplaod> documentUserUplaods = documentDao.getUserDocuments(dateFrom, dateTo);
        return documentUserUplaods;
    }
    
    private Document findByDocId(Long docId)
    {
        Document resultDocument = null;
        final String sql = "SELECT d.* FROM cfs_document d where d.id = ? ";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { docId });
        for (Map row : rows)
        {
            resultDocument = new Document();
            resultDocument.setId((Long) row.get("id"));
            resultDocument.setSharingStatus(SharingStatus.getSharingStatus(String.valueOf(row.get("sharing_status"))));
            
        }
        return resultDocument;
    }
    
    private Document findByDocIdUserId(Long docId, Long userId)
    {
        Document resultDocument = null;
        final String sql = "SELECT d.* , da.parent_id FROM cfs_document d , cfs_doc_access da  where da.doc_id = d.id  and da.doc_id = ? and da.user_id = ? ";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new Object[] { docId, userId });
        if ( rows!=null && rows.size() > 0)
        {
            Map row = rows.get(0);
            resultDocument = new Document();
            resultDocument.setId((Long) row.get("id"));
            resultDocument.setSharingStatus(SharingStatus.getSharingStatus(String.valueOf(row.get("sharing_status"))));
            if (resultDocument.getSharingStatus() == null)
            {
                Long parent_id = (Long) row.get("parent_id");
                if (parent_id != null)
                {
                    Document parent = findByDocId(parent_id);
                    if (parent.getSharingStatus() == null)
                    {
                        parent = findByDocIdUserId(parent_id, userId);
                    }
                    resultDocument.setParent(parent);
                    
                }
                
            }
            
            
        }
        return resultDocument;
    }
    
    public Document.SharingStatus getCorrectSharingStatus(Long docId, Long userId)
    {
        Document document = findByDocIdUserId(docId, userId);
        
        if (document != null && document.getSharingStatus() != null)
        {
            return document.getSharingStatus();
        }
        else
        {
            if (document != null && document.getParent() != null)
            {
                return getCorrectSharingStatus(document.getParent());
            }
            else
            {
                //TODO: should change to public after two weeks
                return Document.SharingStatus.WITH_LINK;
            }
        }
        
    }
    
    public Document.SharingStatus getCorrectSharingStatus(Document document)
    {
        
        if (document.getSharingStatus() != null)
        {
            return document.getSharingStatus();
        }
        else
        {
            //            if (document.getParent() != null)
            //            {
            //                return getCorrectSharingStatus(document.getParent());
            //            }
            //            else
            //            {
            //todo: should change to public after two weeks
            return Document.SharingStatus.WITH_LINK;
            //            }
        }
    }
    
    public List<Document> setCorrectSharingStatus(List<Document> documents)
    {
        for (Document document : documents)
        {
            document.setSharingStatus(getCorrectSharingStatus(document.getId(), AuthenticationUtil.getUserId()));
        }
        return documents;
    }
    
    public List<Document> setCorrectSharingStatus(List<Document> documents,Long userId)
    {
        for (Document document : documents)
        {
            document.setSharingStatus(getCorrectSharingStatus(document.getId(), userId));
        }
        return documents;
    }
    
    public Boolean canAccessToDoc(Document document)
    {
        Boolean access = false;
        
        if ((document.getSharingStatus() != null && (document.getSharingStatus().equals(Document.SharingStatus.PRIVATE))) || (document.isCensored()))
        {
            if(AuthenticationUtil.getUserId() == null)
                access = false;
            
            else if(document.getOwner().getId().equals(AuthenticationUtil.getUserId()))
                access = true;
        }
        else
        {
            access = true;
        }
        
        return access;
    }
    public void setDocumentSyncLocations(Document doc){
        locations = new ArrayList<>();
        List<Location> locations = new ArrayList<>();
        List<DocLocation> docLocations = docLocationDao.findDocLocationByDocId(doc.getId());
        for(DocLocation docLocation : docLocations){
            SyncToken syncToken = syncTokenDao.findSyncByLocDoc(docLocation.getLocation().getId(), doc.getId());
            if(syncToken == null || (syncToken != null && SyncToken.SyncTokenStatus.DONE.equals(syncToken.getSyncTokenStatus()))){
                locations.add(docLocation.getLocation());
            }
        }
        if(docLocations == null || docLocations.size() < 1){
            locations = getDocumentSyncLocationsByParent(doc);
        }
        doc.setLocations(locations);
    }
    
    List<Location> locations = null;
    /*  public List<CfsLocation> getDocumentSyncLocationsByParent(Document doc){
     String path = doc.getFullPath();
     if(path.length()>0){
     List<DocLocation> docLocations = docLocationDao.findDocLocationByFullPath(path);
     for(DocLocation docLocation : docLocations){
     SyncToken syncToken = syncTokenDao.findSyncByLocDoc(docLocation.getCfsLocation().getId(), docLocation.getDocument().getId());
     if(syncToken == null || (syncToken != null && SyncToken.SyncTokenStatus.DONE.equals(syncToken.getSyncTokenStatus()))){
     locations.add(docLocation.getCfsLocation());
     }
     }
     path = path.substring(0, path.lastIndexOf("/"));
     getDocumentSyncLocationsByParent(doc.getParent());
     }
     return locations;
     }*/
    public List<Location> getDocumentSyncLocationsByParent(Document doc)
    {
        
        List<DocLocation> docLocations = docLocationDao.findDocLocationByDocId(doc.getId());
        for(DocLocation docLocation : docLocations){
            SyncToken syncToken = syncTokenDao.findSyncByLocDoc(docLocation.getLocation().getId(), docLocation.getDocument().getId());
            if(syncToken == null || (syncToken != null && SyncToken.SyncTokenStatus.DONE.equals(syncToken.getSyncTokenStatus()))){
                locations.add(docLocation.getLocation());
            }
        }
        getDocumentSyncLocationsByParent(doc.getParent());
        return locations;
    }
    
    //    List<Document> fetchedChild = null;
    //    public List<Document> fetchChildren(Document parent)
    //    {
    //        List<Document> list = ((DocumentDao)getDao()).fetchChildren(parent.getId(), parent.getOwner().getId());
    //        for(Document document: list){
    //            if(document.getDiscriminator().equals(File.DISCRIMINATOR_VALUE))
    //            {
    //                if (fetchedChild == null)
    //                    fetchedChild = new ArrayList<>();
    //                fetchedChild.add(document);
    //            }
    //            else if(document.getDiscriminator().equals(Folder.DISCRIMINATOR_VALUE))
    //            {
    //                fetchChildren(document);
    //            }
    //        }
    //        return fetchedChild;
    //    }
    
    public Long count(Restriction restriction) throws ServiceException
    {
        return getDao().count(restriction);
    }
    
    @Transactional
    @CdnSync
    public List<Document> restore(List<Long>  ids , Long uid) throws ServiceException
    {
        List<Document> documents = new ArrayList<>();
        for(Long id: ids)
        {
            Document document =  documentDao.find(id);
            if(document != null && document.isDeleted())
            {
                if( document == null || ( document.getParent() != null && document.getParent().isDeleted()) ){
                    throw new ServiceException(getMessage("couldNotRestore", null));
                }
                setInnerDocumentProperties(document);
                if(document.getParent() != null)
                {
                    Document anotherDocument  = documentDao.findByDocNameUserIdDicriminatorAndParent(document.getName(), uid, document.getParent().getId(), document.getDiscriminator());
                    if( anotherDocument != null)
                    {
                        throw new ServiceException(getMessage("couldNotRestore", null));
                    }
                }else
                {
                    Document anotherDocument  = documentDao.findByDocNameUserIdDicriminatorWithNullParent(document.getName(), uid, document.getDiscriminator());
                    if( anotherDocument != null)
                    {
                        throw new ServiceException(getMessage("couldNotRestore", null));
                    }
                }
                document.setDeleted(false);
                update(document);
                if(Folder.DISCRIMINATOR_VALUE.equals(document.getDiscriminator()))
                {
                    documentDao.restore(document.getCfsTreePath().concat("%"), document.getOwner().getId(), new Date());
                    
                    //documentEventListener.publish(document);
                }
                documents.add(document);
            }
        }
        return documents;
    }
    
    @Transactional
    @CdnSync
    public void trash(List<Document> entities) throws ServiceException
    {
        for(Document document: entities)
        {
            if(document.isDlc())
            {
                userDlcInvoiceService.isDocumentBought(document);
            }
            document.setDeleted(true);
            update(document);
            if(Folder.DISCRIMINATOR_VALUE.equals(document.getDiscriminator()))
            {
                documentDao.trash(document.getCfsTreePath().concat("%"), document.getOwner().getId(), new Date());
                
                //documentEventListener.publish(document);
                
            }
        }
    }
    
    
    public void restoreToHome(List<Long> ids, Long uid) throws ServiceException
    {
        for (Long id : ids)
        {
            Document document = documentDao.find(id);
            setInnerDocumentProperties(document);
            if (document == null || (document.getParent() != null && document.getParent().isDeleted()))
            {
                throw new ServiceException(getMessage("couldNotRestore", null));
            }
            Document anotherDocument = documentDao.findByDocNameUserIdDicriminatorAndParent(document.getName(), uid, document.getParent().getId(), document.getDiscriminator());
            if (anotherDocument != null)
            {
                throw new ServiceException(getMessage("couldNotRestore", null));
            }
            document.setDeleted(false);
            document.setParent(null);
            update(document);
        }
    }
    
    private class deleteFilesPhysically implements Runnable
    {
        
        private List<Long> deletedIds;
        
        public deleteFilesPhysically(List<Long> deletedIds)
        {
            this.deletedIds = deletedIds;
        }
        
        @Override
        public void run()
        {
            if (CollectionUtils.isNotEmpty(deletedIds))
            {
                for (Long id : deletedIds)
                {
                    fileSystemRepository.deleteQuietlyFromDisk(id);
                }
            }
        }
        
    }
    @Override
    public List<Document> findByIds(List<Long> ids, QueryBuilder qb)
    {
        
        List<Document> entityList = null;
        try
        {
            entityList = getDao().findByIds(ids, new Restriction()
                                            {
                @Override
                public List<Predicate> toPredicate(CriteriaBuilder cb, Root<?> root)
                {
                    List<Predicate> pres = new ArrayList<Predicate>();
                    pres.add(cb.equal(root.get("owner").get("id"), AuthenticationUtil.getUserId()));
                    return pres;
                }
            }, qb);
        }
        catch (ServiceException e)
        {
            logger.info("SERVICE EXCEPTION : {}", e);
        }
        return entityList;
    }
    
    public boolean setRootDocumentProperties(List<Document> docs, Long userId)
    {
        boolean flag = false;
        for(Document doc:docs)
        {
            if(doc.getFullPath() == null || doc.getTreePath() == null)
            {
                flag=true;
                break;
            }
            
        }
        if(flag)
        {
            String query = String.format("select * from fetch_children_of_user(?) ");
            List<Map<String, Object>> rows = getJdbcTemplate().queryForList(query, userId);
            for(Map<String, Object> row:rows)
            {
                documentDao.updateDocumentPath(StringUtil.nullable(row.get("fullpath")), StringUtil.nullable(row.get("treepath")), (long) row.get("id"));
                
                documentEventListener.publish(new Document((long)row.get("id")));
            }
            
        }
        return flag;
    }
    
    
    public void setRootDocumentProperties(List<Document> docs)
    {
        for(Document doc:docs)
        {
            if(doc.getFullPath() == null || doc.getTreePath() == null)
            {
                doc.setCfsFullPath("/".concat(doc.getName()));
                doc.setCfsTreePath(doc.getId().toString());
                try
                {
                    update(doc);
                } catch (ServiceException e)
                {
                    e.printStackTrace();
                }
            }
            
        }
    }
    
    
    public void setInnerDocumentProperties(List<Document> documents, Document parent)
    {
        if(parent.getFullPath() == null || parent.getTreePath()==null)
        {
            setInnerDocumentProperties(parent);
        }
        for(Document doc:documents)
        {
            if(doc.getFullPath() == null || doc.getTreePath() == null)
            {
                doc.setCfsFullPath(parent.getCfsFullPath().concat("/").concat(doc.getName()));
                doc.setCfsTreePath(parent.getCfsTreePath().concat("/").concat(doc.getId().toString()));
                doc.setParent(parent);
                doc.setParentPath(parent.getCfsFullPath());
                try
                {
                    update(doc);
                } catch (ServiceException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    public void setInnerDocumentProperties(List<Document> documents)
    {
        for(Document doc:documents)
        {
            if(doc.getFullPath() == null || doc.getTreePath() == null)
                if(doc.getParent() == null || doc.getParent().getId() == null)
                {
                    doc.setCfsFullPath("/".concat(doc.getName()));
                    doc.setCfsTreePath(doc.getId().toString());
                    try
                    {
                        update(doc);
                    } catch (ServiceException e)
                    {
                        e.printStackTrace();
                    }
                }else
                {
                    
                    String query = "select * from fetch_document_path_up(?) ";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, doc.getId());
                    for (Map<String, Object> row : rows)
                    {
                        String path = (String) row.get("fullpath");
                        
                        String treepath = (String) row.get("treepath");
                        doc.setCfsFullPath(path);
                        doc.setCfsTreePath(treepath);
                        try
                        {
                            update(doc);
                        } catch (ServiceException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
        }
    }
    
    
    public void setInnerDocumentProperties(Document doc)
    {
        if(doc.getFullPath() == null || doc.getTreePath() == null)
            if(doc.getParent() == null || doc.getParent().getId() == null)
            {
                doc.setCfsFullPath("/".concat(doc.getName()));
                doc.setCfsTreePath(doc.getId().toString());
                try
                {
                    update(doc);
                } catch (ServiceException e)
                {
                    e.printStackTrace();
                }
            }else
            {
                
                String query = "select * from fetch_document_path_up(?) ";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, doc.getId());
                for (Map<String, Object> row : rows)
                {
                    String path = (String) row.get("fullpath");
                    
                    String treepath = (String) row.get("treepath");
                    doc.setCfsFullPath(path);
                    doc.setCfsTreePath(treepath);
                    try
                    {
                        update(doc);
                    } catch (ServiceException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
    }
    
    @Transactional
    @CdnSync
    public void renameDocument(Document doc, String docName) throws ServiceException
    {
        String oldName = doc.getName();
        doc.setName(docName);
        String oldPath = doc.getCfsFullPath();
        String newPath = doc.getCfsFullPath().substring(0, doc.getCfsFullPath().lastIndexOf(oldName)) + docName;
        doc.setCfsFullPath(newPath);
        update(doc);
        if (doc.isFolder())
        {
            documentDao.updateFullPath(oldPath, newPath, AuthenticationUtil.getUserId());
            if(doc.getParent() != null)
            {
                doc.getParent().setUpdatedAt(new Date());
                update(doc.getParent());
            }
        }
    }
    @Transactional
    @RefreshCache
    public void unshareWithUsers(Document entity, String userIds) throws ServiceException
    {
        List<Long> transformIds = Lists.transform(StringUtil.split(userIds, ","), new StringToLongTransformer());
        for(long userId:transformIds)
        {
            DocumentAccess documentAccess =  documentAccessDao.findByDocIdUserId(entity.getId(), userId);
            
            if(documentAccess != null)
            {
                documentAccess.setPermission(Permission.DENIED);
                documentAccessService.update(documentAccess);
            }else if(documentDao.docSharedWithUser(userId, entity.getId()))
            {
                User user = new User(userId);
                DocumentAccess docAccess = new DocumentAccess(entity, user, DocumentAccess.Permission.DENIED, false);
                docAccess.setType(Type.SHARE);
                DocAccessId accessId = new DocAccessId(user.getId(), entity.getId());
                docAccess.setDocAccessId(accessId);
                documentAccessDao.save(docAccess);
            }
            
            if(entity.isShared()){
                documentDao.updateUnSharedDocChildWithPath(entity.getId(), userId);
                documentDao.flush();
                documentEventListener.publish(entity);
            }
        }
    }
    
    
    //    @Transactional
    //    public void unshareWithUsers(Document entity, String userIds) throws ServiceException
    //    {
    //        List<Long> transformIds = Lists.transform(StringUtil.split(userIds, ","), new StringToLongTransformer());
    //
    //        for(long userId:transformIds)
    //        {
    //            DocumentAccess documentAccess =  documentAccessDao.findByDocIdUserId(entity.getId(), userId);
    //            if(documentAccess != null)
    //            {
    //                documentAccess.setPermission(Permission.DENIED);
    //                documentAccessService.update(documentAccess);
    //                /*Ashkan*/
    //                List<DocumentAccess> result = documentAccessDao.findNotDeniedByDocId(entity.getId());
    //
    //                if (entity.getDiscriminator().equals(com.cloudst.cfs.model.File.DISCRIMINATOR_VALUE))
    //                {
    //                    if(entity.isShared())
    //                    {
    //                        if(result == null || result.size() == 0){
    //                            entity.setShared(false);
    //                            documentDao.update(entity);
    //                        }
    //                    }
    //                }else{
    //
    //                    List<Document> children = documentDao.getSharedDocumentChildren(entity.getId(), entity.getCfsTreePath().concat("%"), userId);
    //                    for(Document child : children)
    //                    {
    //                        DocumentAccess childDocumentAccess = new DocumentAccess(child, new User(userId), DocumentAccess.Permission.DENIED, false);
    //                        childDocumentAccess.setType(Type.SHARE);
    //                        DocAccessId childAccessId = new DocAccessId(userId, child.getId());
    //                        childDocumentAccess.setDocAccessId(childAccessId);
    //                        documentAccessDao.save(childDocumentAccess);
    //
    //                        if(child.isShared())
    //                        {
    //                            List<DocumentAccess> resultChild = documentAccessDao.findNotDeniedByDocId(child.getId());
    //                            if(resultChild == null || resultChild.size() == 0)
    //                            {
    //                                child.setShared(false);
    //                                documentDao.update(child);
    //                            }
    //                        }
    //                    }
    //
    //                    if(entity.isShared())
    //                    {
    //                        if(result == null || result.size() == 0){
    //                            entity.setShared(false);
    //                            documentDao.update(entity);
    //                        }
    //                    }
    //                }
    //
    //                /*end Ashkan*/
    //
    //            }else if(documentDao.docSharedWithUser(userId, entity.getId())){
    //                User user = new User(userId);
    //                DocumentAccess docAccess = new DocumentAccess(entity, user, DocumentAccess.Permission.DENIED, false);
    //                docAccess.setType(Type.SHARE);
    //                DocAccessId accessId = new DocAccessId(user.getId(), entity.getId());
    //                docAccess.setDocAccessId(accessId);
    //                documentAccessDao.save(docAccess);
    //            }
    //        }
    //    }
    
    
    
    @Transactional(rollbackFor = { Exception.class, ServiceException.class })
    public void saveBatch() throws ServiceException
    {
        Document d1 = new Document();
        d1.setDiscriminator("D");
        d1.setCfsFullPath("/d");
        d1.setName("ddf345122");
        Document d2 = new Document();
        d2.setDiscriminator("D");
        d2.setName("d2353dg");
        
        try {
            insert(d1);
            insert(d2);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        System.out.println(d1.getId());
        throw new ServiceException("fdfd");
    }
    
    @Transactional
    
    public Document createDlcFolder(String jsonContent) throws JSONException, ServiceException
    {
        
        JSONObject json = new JSONObject(jsonContent);
        Axon axon = new AxonBuilder().create();
        
        Document bean = axon.toObject(jsonContent, Document.class, null);
        if(bean.getParent() == null)
        {
            Document doc = documentDao.findByUuid(json.getString("uuid"));
            if(doc != null)
                throw new ServiceException(getMessage("uuidRedundant", null));
            
            if(!json.has("name") )
                bean.setName(json.getString("uuid"));
        }
        bean.setDlc(true);
        insert(bean);
        DlcDocument dlcDocument = new DlcDocument();
        if(json.has("title"))
            dlcDocument.setTitle(json.getString("title"));
        if(json.has("description"))
            dlcDocument.setDescription(json.getString("description"));
        dlcDocument.setDocument(bean);
        dlcDocumentDao.insert(dlcDocument);
        bean.setDlcDocument(dlcDocument);
        bean = documentDao.update(bean);
        
        //documentEventListener.publish(bean);
        
        return bean;
    }
    
    public List<Document> getUserDeletedDocuments(Long userID)
    {
        String query = "select * from fetch_deleted_documents_by_user_id(?) ";
        return mapToDocumentsLightVersion( jdbcTemplate.queryForList(query, userID));
    }
    
    public List<Document> getUserDeletedDocuments(Long userID, boolean dlc, boolean cdn)
    {
        String query = "select * from fetch_deleted_documents_by_user_id(?, ?, ?) ";
        return mapToDocumentsLightVersion( jdbcTemplate.queryForList(query, userID, dlc, cdn));
    }
    
    public List<Document> mapToDocumentsLightVersion(List<Map<String, Object>> rows)
    {
        List<Document> docs = new ArrayList<Document>();
        for (Map<String, Object> row : rows)
        {
            Object owner = row.get("user_id");
            if (owner == null)
                owner = row.get("user_id");
            try
            {
                User u = userDao.find((Long)owner);
                Object pid = row.get("parent_id");
                Document parent = null;
                if (pid != null)
                {
                    parent = new Document((Long) pid);
                }
                Document d = new Document((long) row.get("id"), u, parent, StringUtil.nullable(row.get("cfs_fullpath")),StringUtil.nullable(row.get("cfs_treepath")),
                                          (String) row.get("properties"), (String) row.get("name"),
                                          (String) row.get("discriminator"), (String) row.get("uuid"), (Long) row.get("size"),
                                          (String) row.get("created_by"), (Date) row.get("updated_at"), (Date) row.get("created_at"),
                                          (String) row.get("extension"), (String) row.get("mime_type"), (Boolean) row.get("shared"));
                
                d.setDescription((String) row.get("description"));
                d.setBoost((BigDecimal) row.get("boost"));
                
                docs.add(d);
            }
            catch (Exception e)
            {
                logger.info("DocumentService mapToDocuments failed");
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
        return docs;
    }
    public void deleteFromTrash()
    {
        List<Long> userIds = documentDao.getTrashFilesOwner();
        for(Long uid : userIds)
        {
            //Long uid = 46005680L;
            
            String query = "select * from fetch_old_deleted_documents_by_user_id(?) ";
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, uid);
            StringBuffer ids = new StringBuffer();
            //setDeletedDocumentProperties(rows);
            for(Map<String, Object> row : rows)
            {
                long id = (long) row.get("id");
                ids.append(id).append(",");
            }
            try
            {
                if(ids != null && ids.length() > 0)
                {
                    deleteDoc(ids.toString(), uid);
                    documentAccessDao.updateUserSpace(uid);
                }
            } catch (ServiceException e)
            {
                e.printStackTrace();
            }
        }
    }
    public String getUserRootFilesName(Long userId, Long parentId)
    {
        return documentDao.getUserRootFilesName(userId, parentId);
    }
    public void setDeletedDocumentProperties(List<Map<String, Object>> docs)
    {
        
        for(Map<String, Object> doc : docs)
        {
            if(doc.get("cfs_treepath") == null || StringUtil.isEmpty((String)doc.get("cfs_treepath")))
            {
                
                long id = (long) doc.get("id");
                String query = "select * from fetch_document_path_up(?) ";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, id);
                for (Map<String, Object> row : rows)
                {
                    documentDao.updateDocumentPath(StringUtil.nullable(row.get("fullpath")), StringUtil.nullable(row.get("treepath")), (long) row.get("id"));
                }
            }
        }
    }
    
    public org.codehaus.jettison.json.JSONArray pushSyncSftpCdn(String metadata, String cdnUsername, Long userId) throws ServiceException, JsonParseException, JsonMappingException, IOException{
        
        org.codehaus.jettison.json.JSONArray diff = new org.codehaus.jettison.json.JSONArray();
        
        if(StringUtils.isEmpty(cdnUsername))
            throw new ServiceException(getMessage("cdnUsernameIsEmpty", null));
        ObjectMapper mapper = new ObjectMapper();
        Metadata meta = mapper.readValue(metadata, Metadata.class);
        
        CdnAccount cdn = cdnAccountDao.findByUsername(cdnUsername, userId);
        if(cdn == null)
            throw new ServiceException(getMessage("noCdnUsernameexists", null));
        
        String treePath = cdn.getSubdomain().getDocument().getCfsTreePath().concat("%");
        List<Document> docs = documentDao.findCDNDocuments(treePath, userId);
        
        for(LinkedHashMap<String, String> lhm : meta.attributes){
            String fullpath = lhm.get("fullpath");
            String name = lhm.get("name");
            String type = lhm.get("type");
            String path = lhm.get("path");
            Timestamp mod = Timestamp.valueOf(lhm.get("mod").replace(".0", "")); //(Long.parseLong(lhm.get("mod").replace(".0", "")));
            Long size = (!StringUtils.isEmpty(lhm.get("size"))) ? new Long(lhm.get("size")) : 0L;
            
            Boolean found = false;
            Timestamp updatedAt = mod;
            Document foundDoc = null;
            Long docSize = 0l;
            for(Document doc : docs){
                if(doc.getFullPath().equals(fullpath)){
                    found = true;
                    updatedAt = new Timestamp(doc.getUpdatedAt().getTime());
                    docSize = doc.getSize();
                    foundDoc = doc;
                    break;
                }
            }
            
            if(!found){
                if(type.equals(File.DISCRIMINATOR_VALUE)){
                    
                    /* cfs doesn't have this file, so it will be added to diff array to be uploaded later by sftp*/
                    
                    Document parent = documentDao.findByFullPathUserId(path, AuthenticationUtil.getUserId());
                    org.codehaus.jettison.json.JSONObject obj = new org.codehaus.jettison.json.JSONObject();
                    try {
                        obj.put("name", lhm.get("fullpath"));
                        obj.put("parent_id", parent.getId());
                        diff.put(obj);
                    } catch (org.codehaus.jettison.json.JSONException e) {
                        e.printStackTrace();
                    }
                    
                }else{
                    /* cfs doesn't have this folder, so it will be inserted*/
                    
                    StringTokenizer tokenizer = new StringTokenizer(fullpath, "/");
                    String temp = "";
                    int i = 0;
                    while(tokenizer.hasMoreTokens() && i < tokenizer.countTokens()){
                        temp = temp  + "/" + tokenizer.nextToken();
                        i++;
                    }
                    
                    Document parent = documentDao.findByFullPathUserId(path, AuthenticationUtil.getUserId());
                    Document doc = new Document();
                    doc.setName(name);
                    doc.setParent(parent);
                    doc.setDiscriminator(type);
                    doc.setSubdomain(true);
                    insertInPushSyncMode(doc);
                    //insert(doc);
                }
            }else{
                if(type.equals(File.DISCRIMINATOR_VALUE)){
                    if(mod.compareTo(updatedAt) > 0 && size != docSize){
                        
                        /* if mod time of sftp is greater than updatedAt of cfs, it will be added to diff array to be uploaded later by sftp*/
                        
                        Document parent = documentDao.findByFullPathUserId(path, AuthenticationUtil.getUserId());
                        org.codehaus.jettison.json.JSONObject obj = new org.codehaus.jettison.json.JSONObject();
                        try {
                            obj.put("name", lhm.get("fullpath"));
                            obj.put("parent_id", parent.getId());
                            diff.put(obj);
                            documentDao.remove(foundDoc);
                        } catch (org.codehaus.jettison.json.JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    
                }
            }
            
        }
        
        List<Document> delDocs = new ArrayList<Document>();
        for(Document doc : docs){
            Boolean found = false;
            for(LinkedHashMap<String, String> lhm : meta.attributes){
                String fullpath = lhm.get("fullpath");
                if(doc.getCfsFullPath().equals(fullpath)){
                    found = true;
                    break;
                }
            }
            if(!found){
                /* if the document doesen't exists in sftp, it will be removed*/
                delDocs.add(doc);
            }
        }
        Collections.sort(delDocs, new Comparator<Document>() {
            
            @Override
            public int compare(Document o1, Document o2) {
                return o2.getFullPath().length() > o1.getFullPath().length() ? 1 : -1;
            }
        });
        
        for(Document doc : delDocs)
            recursiveDelete(doc.getId());
        
        return diff;
    }
    
    public org.codehaus.jettison.json.JSONArray pullSyncSftpCdn(String cdnUsername, Long userId) throws ServiceException, JsonParseException, JsonMappingException, IOException{
        org.codehaus.jettison.json.JSONArray all = new org.codehaus.jettison.json.JSONArray();
        if(StringUtils.isEmpty(cdnUsername))
            throw new ServiceException(getMessage("cdnUsernameIsEmpty", null));
        
        CdnAccount cdn = cdnAccountDao.findByUsername(cdnUsername, userId);
        if(cdn == null)
            throw new ServiceException(getMessage("noCdnUsernameexists", null));
        
        String query = "select cfs_fullpath, uuid, discriminator, updated_at, COALESCE(size,0) as size from cfs_document where cfs_treepath like ?||'%' and user_id = ? and subdomain = true and deleted = false";
        List<Map<String, Object>> rows = getJdbcTemplate().queryForList(query, cdn.getSubdomain().getDocument().getId(), userId);
        
        for(Map<String, Object> map : rows){
            
            org.codehaus.jettison.json.JSONObject obj = new org.codehaus.jettison.json.JSONObject();
            try {
                obj.put("name", map.get("cfs_fullpath"));
                obj.put("uuid", map.get("uuid"));
                obj.put("type", map.get("discriminator"));
                obj.put("updatedAt", ((Timestamp)map.get("updated_at")).getTime());
                obj.put("size", map.get("size"));
                all.put(obj);
            } catch (org.codehaus.jettison.json.JSONException e) {
                e.printStackTrace();
            }
        }
        
        return all;
    }
    
    @CdnSync
    public Document doSyncInPullMode(Document entity){
        return entity;
    }
    
    @Transactional
    public Document insertInPushSyncMode(Document entity) throws ServiceException{
        
        if (entity.getDiscriminator() == null)
        {
            entity.setDiscriminator(Folder.DISCRIMINATOR_VALUE);
        }
        if (!entity.getDiscriminator().equals(Folder.DISCRIMINATOR_VALUE))
        {
            throw new ServiceException(getMessage("onlyFolderOrFile", null));
            
        }
        
        User currentUser = userDao.find(AuthenticationUtil.getUserId());
        Document parent = retrieveParent(entity);
        entity.setParent(parent);
        entity.setCfsFullPath((entity.getParent() != null && entity.getParent().getCfsFullPath() != null) ? entity.getParent().getCfsFullPath().concat("/").concat(entity.getName()) : "/".concat(entity.getName()));
        entity.setOwner(currentUser);
        if (entity.getParent() == null)
        {
            //User currentUser = userDao.findByUsername(AuthenticationUtil.getUsername());
            entity.setSharingStatus(currentUser.getCfsRootSharingStatus());
        }
        // also see documentVO#prepersist mehtod
        entity.setOwner(currentUser);
        if(entity.getParent() != null && entity.getParent().isShared())
            entity.setShared(true);
        StringEscapeUtils.escapeXml(entity.getDescription());
        StringEscapeUtils.escapeHtml4(entity.getDescription());
        
        documentDao.insert(entity);
        entity.setCfsTreePath((entity.getParent() != null && entity.getParent().getCfsTreePath() != null) ? entity.getParent().getCfsTreePath().concat("/").concat(entity.getId().toString()) : (entity.getId().toString()));
        update(entity);
        
        return entity;
    }
}

