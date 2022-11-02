package cube.client.test;

import cell.util.Utils;
import cube.client.Client;
import cube.common.entity.Contact;
import cube.common.entity.ContactBehavior;
import cube.common.entity.SharingTag;
import cube.common.entity.VisitTrace;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DemoClient {


    public static void main(String[] args) {
        System.out.println("Start");

        Client client = new Client("127.0.0.1", "admin", "shixincube.com");

        if (!client.waitReady()) {
            System.err.println("Error");
            return;
        }

        // 获取联系人
        Contact contact = client.getContact("shixincube.com", 50001001);
        System.out.println("ID: " + contact.getId());
        System.out.println("Name: " + contact.getName());

        System.out.println("----------------------------------------------------");

        long beginTime = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        List<ContactBehavior> behaviors = client.listContactBehaviors(contact.getId(), contact.getDomain().getName(),
                beginTime, System.currentTimeMillis());
        System.out.println("Behavior size: " + behaviors.size());
        for (ContactBehavior behavior : behaviors) {
            System.out.println("Behavior: " + behavior.getBehavior());
            System.out.println("Time: " + Utils.gsDateFormat.format(new Date(behavior.getTimestamp())));
        }

        /*
        List<SharingTag> list = client.getFileStorage().listSharingTags(contact.getId(), contact.getDomain().getName(),
                0, 9, true);
        System.out.println("Size: " + list.size());
        for (SharingTag tag : list) {
            System.out.println("Code: " + tag.getCode());
            System.out.println("Time: " + Utils.gsDateFormat.format(new Date(tag.getTimestamp())));
            System.out.println("File name: " + tag.getConfig().getFileLabel().getFileName());
            System.out.println("File size: " + tag.getConfig().getFileLabel().getFileSize());
        }*/

        /*
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(Calendar.DATE, 24);
        beginTime.set(Calendar.HOUR, 0);
        List<VisitTrace> visitTraceList = client.getFileStorage().searchVisitTraces(contact.getId(), contact.getDomain().getName(),
                beginTime.getTimeInMillis(), System.currentTimeMillis());
        System.out.println("Size: " + visitTraceList.size());
        for (VisitTrace visitTrace : visitTraceList) {
            System.out.println("Time: " + Utils.gsDateFormat.format(new Date(visitTrace.time)));
            System.out.println("UserAgent: " + visitTrace.userAgent);
            System.out.println("Event: " + visitTrace.event);
        }*/

        /*
        FileStoragePerformance performance = client.getFileStorage().getStoragePerformance(contact.getId(), contact.getDomain().getName());
        System.out.println("performance: \n" + performance.toJSON().toString(4));

        performance.setMaxSpaceSize(4294967296L);
        // 更新
        performance = client.getFileStorage().updateStoragePerformance(contact.getId(), contact.getDomain().getName(), performance);
        System.out.println("new performance: \n" + performance.toJSON().toString(4));
        */

        System.out.println("End");
        client.destroy();
    }

}
