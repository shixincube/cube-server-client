package cube.client.file;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.client.Client;
import cube.common.action.ClientAction;
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

    public FileStoragePerformance getStoragePerformance(String domain, long contactId) {
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

    public FileStoragePerformance setStoragePerformance(String domain, long contactId,
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

    /**
     * 批量获取分享标签。
     *
     * @param contactId
     * @param domainName
     * @param beginIndex
     * @param endIndex
     * @param valid
     * @return
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
     * @param sharingCode
     * @return
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
     * 获取分享标签总数量。
     *
     * @param contactId
     * @param valid
     * @return
     */
    public int getSharingTagTotal(long contactId, boolean valid) {
        Integer value = valid ? this.validSharingTagTotalMap.get(contactId) :
                this.invalidSharingTagTotalMap.get(contactId);
        if (null == value) {
            return 0;
        }

        return value.intValue();
    }

    /**
     * 批量获取文件访问痕迹。
     *
     * @param contactId
     * @param domainName
     * @param sharingCode
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<VisitTrace> listVisitTrace(long contactId, String domainName, String sharingCode,
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
                Logger.w(this.getClass(), "#listVisitTrace - Network error");
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
     * @param sharingCode
     * @return
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
