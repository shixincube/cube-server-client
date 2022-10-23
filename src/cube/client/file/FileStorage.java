package cube.client.file;

import cell.core.talk.dialect.ActionDialect;
import cube.client.Client;
import cube.common.action.ClientAction;
import cube.common.entity.FileStoragePerformance;
import cube.common.state.FileStorageStateCode;
import org.json.JSONObject;

public class FileStorage {

    private Client client;

    public FileStorage(Client client) {
        this.client = client;
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
}
