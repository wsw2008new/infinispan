package org.infinispan.xsite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import org.infinispan.commands.remote.BaseRpcCommand;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.context.InvocationContext;
import org.infinispan.util.ByteString;

/**
 * Command used for handling XSiteReplication administrative operations.
 *
 * @author Mircea Markus
 * @since 5.2
 */
public class XSiteAdminCommand extends BaseRpcCommand {

   public static final int COMMAND_ID = 32;

   public enum AdminOperation {
      SITE_STATUS,
      STATUS,
      TAKE_OFFLINE,
      BRING_ONLINE,
      AMEND_TAKE_OFFLINE;

      private static final AdminOperation[] CACHED_VALUES = values();

      private static AdminOperation valueOf(int index) {
         return CACHED_VALUES[index];
      }
   }

   public enum Status {
      OFFLINE, ONLINE
   }

   private String siteName;
   private Integer afterFailures;
   private Long minTimeToWait;
   private AdminOperation adminOperation;

   private BackupSender backupSender;

   @SuppressWarnings("unused")
   public XSiteAdminCommand() {
      super(null);// For command id uniqueness test
   }

   public XSiteAdminCommand(ByteString cacheName) {
      super(cacheName);// For command id uniqueness test
   }

   public XSiteAdminCommand(ByteString cacheName, String siteName, AdminOperation op, Integer afterFailures, Long minTimeToWait) {
      this(cacheName);
      this.siteName = siteName;
      this.adminOperation = op;
      this.afterFailures = afterFailures;
      this.minTimeToWait = minTimeToWait;
   }

   public void init(BackupSender backupSender) {
      this.backupSender = backupSender;
   }

   @Override
   public Object perform(InvocationContext ctx) throws Throwable {
      switch (adminOperation) {
         case SITE_STATUS: {
            if (backupSender.getOfflineStatus(siteName).isOffline()) {
               return Status.OFFLINE;
            } else {
               return Status.ONLINE;
            }
         }
         case STATUS: {
            return backupSender.status();
         }
         case TAKE_OFFLINE: {
            return backupSender.takeSiteOffline(siteName);
         }
         case BRING_ONLINE: {
            return backupSender.bringSiteOnline(siteName);
         }
         case AMEND_TAKE_OFFLINE: {
            backupSender.getOfflineStatus(siteName).amend(afterFailures, minTimeToWait);
            return null;
         }
         default: {
            throw new IllegalStateException("Unhandled admin operation " + adminOperation);
         }
      }
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void writeTo(ObjectOutput output) throws IOException {
      MarshallUtil.marshallEnum(adminOperation, output);
      switch (adminOperation) {
         case SITE_STATUS:
         case TAKE_OFFLINE:
         case BRING_ONLINE:
            output.writeUTF(siteName);
            return;
         case AMEND_TAKE_OFFLINE:
            output.writeUTF(siteName);
            output.writeObject(afterFailures);
            output.writeObject(minTimeToWait);
            return;
         case STATUS:
            return;
         default:
            throw new IllegalStateException("Unknown admin operation " + adminOperation);
      }
   }

   @Override
   public void readFrom(ObjectInput input) throws IOException, ClassNotFoundException {
      adminOperation = Objects.requireNonNull(MarshallUtil.unmarshallEnum(input, AdminOperation::valueOf));
      switch (adminOperation) {
         case SITE_STATUS:
         case TAKE_OFFLINE:
         case BRING_ONLINE:
            siteName = input.readUTF();
            return;
         case AMEND_TAKE_OFFLINE:
            siteName = input.readUTF();
            afterFailures = (Integer) input.readObject();
            minTimeToWait = (Long) input.readObject();
            return;
         case STATUS:
            return;
         default:
            throw new IllegalStateException("Unknown admin operation " + adminOperation);
      }
   }

   @Override
   public final boolean isReturnValueExpected() {
      return true;
   }

   @Override
   public String toString() {
      return "XSiteAdminCommand{" +
            "siteName='" + siteName + '\'' +
            ", afterFailures=" + afterFailures +
            ", minTimeToWait=" + minTimeToWait +
            ", adminOperation=" + adminOperation +
            ", backupSender=" + backupSender +
            '}';
   }
}
