package org.infinispan.remoting.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mircea Markus
 * @since 5.2
 */
public class AggregateBackupResponse implements BackupResponse {

   final Collection<BackupResponse> responses;

   public AggregateBackupResponse(BackupResponse onePcResponse, BackupResponse twoPcResponse) {
      responses = new ArrayList<BackupResponse>(2);
      if (onePcResponse != null) responses.add(onePcResponse);
      if (twoPcResponse != null) responses.add(twoPcResponse);
   }

   @Override
   public void waitForBackupToFinish() throws Exception {
      for (BackupResponse br : responses) {
         br.waitForBackupToFinish();
      }
   }

   @Override
   public Map<String, Throwable> getFailedBackups() {
      Map<String, Throwable> result = new HashMap<String, Throwable>();
      for (BackupResponse br : responses) {
         result.putAll(br.getFailedBackups());
      }
      return result;
   }

   @Override
   public Set<String> getCommunicationErrors() {
      Set<String> result = new HashSet<String>();
      for (BackupResponse br : responses) {
         result.addAll(br.getCommunicationErrors());
      }
      return result;
   }

   @Override
   public long getSendTimeMillis() {
      long min = Long.MAX_VALUE;
      for (BackupResponse br: responses) {
         min = Math.min(br.getSendTimeMillis(), min);
      }
      return min;
   }

   @Override
   public String toString() {
      return "AggregateBackupResponse{" +
            "responses=" + responses +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof AggregateBackupResponse)) return false;

      AggregateBackupResponse that = (AggregateBackupResponse) o;

      if (responses != null ? !responses.equals(that.responses) : that.responses != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return responses != null ? responses.hashCode() : 0;
   }

   @Override
   public boolean isEmpty() {
      for (BackupResponse br : responses) {
         if (!br.isEmpty()) return false;
      }
      return true;
   }
}
