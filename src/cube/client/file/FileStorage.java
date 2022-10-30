package cube.client.file;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.client.ActionListener;
import cube.client.Client;
import cube.client.Notifier;
import cube.common.action.ClientAction;
import cube.common.entity.FileLabel;
import cube.common.entity.FileStoragePerformance;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;
import cube.common.notice.GetSharingTag;
import cube.common.notice.ListSharingTags;
import cube.common.notice.ListSharingTraces;
import cube.common.notice.NoticeData;
import cube.common.state.FileStorageStateCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件存储服务访问接口。
 */
public class FileStorage {

    private Client client;

    private Map<Long, Integer> validSharingTagTotalMap;
    private Map<Long, Integer> invalidSharingTagTotalMap;

    /**
     * 分享标签对应的访问记录总数。
     */
    private Map<String, Integer> visitTraceTotalMap;

    public FileStorage(Client client) {
        this.client = client;
        this.validSharingTagTotalMap = new HashMap<>();
        this.invalidSharingTagTotalMap = new HashMap<>();
        this.visitTraceTotalMap = new HashMap<>();
    }

    /**
     * 获取指定联系人的存储性能。
     *
     * @param contactId 指定联系人 ID 。
     * @param domain 指定访问域。
     * @return 返回文件存储性能数据实例。
     */
    public FileStoragePerformance getStoragePerformance(long contactId, String domain) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetFilePerf.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("contactId", contactId);

        ActionDialect result = this.client.syncTransmit(actionDialect);
        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject performance = result.getParamAsJson("performance");
            return new FileStoragePerformance(performance);
        }
        else {
            return null;
        }
    }

    /**
     * 更新联系人的存储性能。
     *
     * @param contactId 指定联系人 ID 。
     * @param domain 指定访问域。
     * @param performance 指定新的性能数据。
     * @return 返回已更新的性能数据，如果更新失败返回 {@code null} 值。
     */
    public FileStoragePerformance updateStoragePerformance(long contactId, String domain,
                                                           FileStoragePerformance performance) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.UpdateFilePerf.name);
        actionDialect.addParam("domain", domain);
        actionDialect.addParam("contactId", contactId);
        actionDialect.addParam("performance", performance.toJSON());

        ActionDialect result = this.client.syncTransmit(actionDialect);
        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject performanceJson = result.getParamAsJson("performance");
            return new FileStoragePerformance(performanceJson);
        }
        else {
            return null;
        }
    }

    public List<FileLabel> searchFiles(long contactId, String domainName, long beginTime, long endTime) {
        return null;
    }

    /**
     * 按照时间检索分享标签数据。
     *
     * @param contactId 指定联系人 ID 。
     * @param domainName 指定访问域名称。
     * @param beginTime 指定开始时间戳。
     * @param endTime 指定结束时间戳。
     * @param valid 指定是否是有效的标签。
     * @return 返回搜索到的分享标签列表，如果查找时发生错误返回 {@code null} 值。
     */
    public List<SharingTag> searchSharingTags(long contactId, String domainName, long beginTime, long endTime, boolean valid) {
        if (endTime <= beginTime) {
            return null;
        }

        List<SharingTag> list = new ArrayList<>();

        AtomicInteger total = new AtomicInteger(0);
        Notifier notifier = this.client.getReceiver().inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTags.name);
        actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTags(contactId, domainName, beginTime,
                endTime, valid));

        ActionListener actionListener = new ActionListener() {
            @Override
            public void onAction(ActionDialect actionDialect) {
                JSONObject notifierJson = actionDialect.getParamAsJson(Notifier.AsyncParamName);
                if (notifier.equals(notifierJson)) {
                    JSONObject data = actionDialect.getParamAsJson("data");
                    SharingTag tag = new SharingTag(data);
                    list.add(tag);

                    if (total.get() == list.size()) {
                        synchronized (list) {
                            list.notify();
                        }
                    }
                }
            }
        };
        this.client.getReceiver().addActionListener(ClientAction.ListSharingTags.name, actionListener);

        ActionDialect result = this.client.getConnector().send(notifier, actionDialect);
        if (null == result) {
            // 移除监听器
            this.client.getReceiver().removeActionListener(ClientAction.ListSharingTags.name, actionListener);
            Logger.w(this.getClass(), "#searchSharingTags - Network error");
            return null;
        }

        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject data = result.getParamAsJson("data");
            total.set(data.getInt("total"));
        }

        if (total.get() == 0) {
            // 移除监听器
            this.client.getReceiver().removeActionListener(ClientAction.ListSharingTags.name, actionListener);
            return list;
        }

        synchronized (list) {
            try {
                list.wait(60 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 移除监听器
        this.client.getReceiver().removeActionListener(ClientAction.ListSharingTags.name, actionListener);
        return list;
    }

    /**
     * 批量获取分享标签。
     *
     * @param contactId 指定联系人 ID 。
     * @param domainName 指定访问域。
     * @param beginIndex 指定起始索引。
     * @param endIndex 指定结束索引。
     * @param valid 指定获取的是否是有效的分享标签。
     * @return 返回分享标签列表。
     */
    public List<SharingTag> listSharingTags(long contactId, String domainName,
                                            int beginIndex, int endIndex, boolean valid) {
        List<SharingTag> list = new ArrayList<>();

        final int step = 10;
        List<Integer> indexes = new ArrayList<>();
        int delta = endIndex - beginIndex;
        if (delta > 9) {
            int num = (int) Math.floor((float)(delta + 1) / (float)step);
            int mod = (delta + 1) % step;
            int index = beginIndex;
            for (int i = 0; i < num; ++i) {
                index += step - 1;
                indexes.add(index);
                index += 1;
            }

            if (mod != 0) {
                index += mod - 1;
                indexes.add(index);
            }
        }
        else {
            indexes.add(endIndex);
        }

        int begin = beginIndex;
        int end = 0;
        for (Integer index : indexes) {
            end = index;

            ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTags.name);
            actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTags(contactId, domainName,
                    begin, end, valid));

            ActionDialect result = this.client.syncTransmit(actionDialect);
            if (null == result) {
                Logger.w(this.getClass(), "#listSharingTags - Network error");
                break;
            }

            if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
                JSONObject data = result.getParamAsJson("data");
                JSONArray array = data.getJSONArray("list");
                if (array.length() == 0) {
                    // 没有数据，结束数据获取
                    continue;
                }

                for (int i = 0; i < array.length(); ++i) {
                    SharingTag tag = new SharingTag(array.getJSONObject(i));
                    list.add(tag);
                }

                if (valid) {
                    this.validSharingTagTotalMap.put(contactId, data.getInt("total"));
                }
                else {
                    this.invalidSharingTagTotalMap.put(contactId, data.getInt("total"));
                }
            }

            // 更新索引
            begin = index + 1;
        }

        return list;
    }

    /**
     * 获取分享标签。
     *
     * @param sharingCode 获取指定访问码的分享标签。
     * @return 返回分享标签实例。查找失败返回 {@code null} 值。
     */
    public SharingTag getSharingTag(String sharingCode) {
        ActionDialect actionDialect = new ActionDialect(ClientAction.GetSharingTag.name);
        actionDialect.addParam(NoticeData.PARAMETER, new GetSharingTag(sharingCode));

        ActionDialect result = this.client.syncTransmit(actionDialect);
        if (null == result) {
            Logger.w(this.getClass(), "#getSharingTag - Network error");
            return null;
        }

        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject data = result.getParamAsJson("data");
            return new SharingTag(data);
        }
        else {
            return null;
        }
    }

    /**
     * 从本地缓存里获取分享标签总数量。
     *
     * @param contactId 指定联系人 ID 。
     * @param valid 是否是有效分享标签。
     * @return 返回数量，如果没有找到已缓存的联系人返回 {@code -1} 值。
     */
    public int getCachedSharingTagTotal(long contactId, boolean valid) {
        Integer value = valid ? this.validSharingTagTotalMap.get(contactId) :
                this.invalidSharingTagTotalMap.get(contactId);
        if (null == value) {
            return -1;
        }

        return value.intValue();
    }

    public List<VisitTrace> getVisitTrace(String sharingCode) {
        return null;
    }

    /**
     * 按照时间检索访问痕迹数据。
     *
     * @param contactId 指定联系人 ID 。
     * @param domainName 指定访问域名称。
     * @param beginTime 指定开始时间戳。
     * @param endTime 指定结束时间戳。
     * @return 返回搜索到的访问记录列表，如果查找时发生错误返回 {@code null} 值。
     */
    public List<VisitTrace> searchVisitTraces(long contactId, String domainName, long beginTime, long endTime) {
        if (endTime <= beginTime) {
            return null;
        }

        List<VisitTrace> list = new ArrayList<>();

        AtomicInteger total = new AtomicInteger(0);
        Notifier notifier = this.client.getReceiver().inject();

        ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTraces.name);
        actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTraces(contactId, domainName, beginTime, endTime));

        ActionListener actionListener = new ActionListener() {
            @Override
            public void onAction(ActionDialect actionDialect) {
                JSONObject notifierJson = actionDialect.getParamAsJson(Notifier.AsyncParamName);
                if (notifier.equals(notifierJson)) {
                    JSONObject data = actionDialect.getParamAsJson("data");
                    VisitTrace trace = new VisitTrace(data);
                    list.add(trace);

                    if (total.get() == list.size()) {
                        synchronized (list) {
                            list.notify();
                        }
                    }
                }
            }
        };
        this.client.getReceiver().addActionListener(ClientAction.ListSharingTraces.name, actionListener);

        ActionDialect result = this.client.getConnector().send(notifier, actionDialect);
        if (null == result) {
            // 移除监听器
            this.client.getReceiver().removeActionListener(ClientAction.ListSharingTraces.name, actionListener);
            Logger.w(this.getClass(), "#searchVisitTraces - Network error");
            return null;
        }

        if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
            JSONObject data = result.getParamAsJson("data");
            total.set(data.getInt("total"));
        }

        if (total.get() == 0) {
            // 移除监听器
            this.client.getReceiver().removeActionListener(ClientAction.ListSharingTraces.name, actionListener);
            return list;
        }

        synchronized (list) {
            try {
                list.wait(60 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 移除监听器
        this.client.getReceiver().removeActionListener(ClientAction.ListSharingTraces.name, actionListener);
        return list;
    }

    /**
     * 批量获取文件访问痕迹。
     *
     * @param contactId 指定联系人 ID 。
     * @param domainName 指定访问域名称。
     * @param sharingCode 指定分享码。
     * @param beginIndex 指定起始索引位置。
     * @param endIndex 指定结束索引位置。
     * @return 返回数据列表。
     */
    public List<VisitTrace> listVisitTraces(long contactId, String domainName, String sharingCode,
                                           int beginIndex, int endIndex) {
        List<VisitTrace> list = new ArrayList<>();

        final int step = 10;
        List<Integer> indexes = new ArrayList<>();
        int delta = endIndex - beginIndex;
        if (delta > 9) {
            int num = (int) Math.floor((float)(delta + 1) / (float)step);
            int mod = (delta + 1) % step;
            int index = beginIndex;
            for (int i = 0; i < num; ++i) {
                index += step - 1;
                indexes.add(index);
                index += 1;
            }

            if (mod != 0) {
                index += mod - 1;
                indexes.add(index);
            }
        }
        else {
            indexes.add(endIndex);
        }

        int begin = beginIndex;
        int end = 0;
        for (Integer index : indexes) {
            end = index;

            ActionDialect actionDialect = new ActionDialect(ClientAction.ListSharingTraces.name);
            actionDialect.addParam(NoticeData.PARAMETER, new ListSharingTraces(contactId, domainName, sharingCode,
                    begin, end));

            ActionDialect result = this.client.syncTransmit(actionDialect);
            if (null == result) {
                Logger.w(this.getClass(), "#listVisitTraces - Network error");
                break;
            }

            if (result.getParamAsInt("code") == FileStorageStateCode.Ok.code) {
                JSONObject data = result.getParamAsJson("data");
                JSONArray array = data.getJSONArray("list");
                if (array.length() == 0) {
                    // 没有更多数据
                    break;
                }

                for (int i = 0; i < array.length(); ++i) {
                    VisitTrace trace = new VisitTrace(array.getJSONObject(i));
                    list.add(trace);
                }

                // 总数
                synchronized (this.visitTraceTotalMap) {
                    this.visitTraceTotalMap.put(sharingCode, data.getInt("total"));
                }
            }

            // 更新索引
            begin = index + 1;
        }

        return list;
    }

    /**
     * 获取访问痕迹总数量。
     *
     * @param sharingCode 指定分享标签的分享码。
     * @return 返回访问痕迹总数量。
     */
    public int getVisitTraceTotal(String sharingCode) {
        synchronized (this.visitTraceTotalMap) {
            Integer value = this.visitTraceTotalMap.get(sharingCode);
            if (null == value) {
                return 0;
            }

            return value.intValue();
        }
    }
}
