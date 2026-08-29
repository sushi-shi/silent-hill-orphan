package javax.microedition.rms;

public class RecordStore {
    public static String oracleDeleteName;
    public static int oracleDeleteCalls;
    public static int oracleDeleteMode;

    private RecordStore() {}

    public static RecordStore openRecordStore(String name, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        return new RecordStore();
    }

    public static void deleteRecordStore(String name)
            throws RecordStoreException, RecordStoreNotFoundException {
        oracleDeleteCalls++;
        oracleDeleteName = name;
        if (oracleDeleteMode == 1) {
            throw new RecordStoreNotFoundException("injected missing store");
        }
        if (oracleDeleteMode == 2) {
            throw new RecordStoreException("injected record-store failure");
        }
        if (oracleDeleteMode == 3) {
            throw new NullPointerException("injected unchecked failure");
        }
    }

    public static void oracleResetDelete(int mode) {
        oracleDeleteName = null;
        oracleDeleteCalls = 0;
        oracleDeleteMode = mode;
    }

    public void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {}

    public int getNumRecords() throws RecordStoreNotOpenException {
        return 0;
    }

    public int getSizeAvailable() throws RecordStoreNotOpenException {
        return 0;
    }

    public int addRecord(byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        return 0;
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {}

    public void deleteRecord(int recordId)
            throws RecordStoreNotOpenException, RecordStoreException {}

    public byte[] getRecord(int recordId)
            throws RecordStoreNotOpenException, RecordStoreException {
        return null;
    }
}
