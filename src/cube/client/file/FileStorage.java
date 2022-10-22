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

//    public void setMaxSpaceSize(String domain, long contactId, long maxSize) {
//        ActionDialect actionDialect = new ActionDialect(ClientAction.UpdateFilePerf.name);
//        actionDialect.addParam("domain", domain);
//        actionDialect.addParam("contactId", contactId);
//        actionDialect.addParam("maxSpaceSize", maxSize);
//    }
}
