package javax.microedition.rms;

public class RecordStore {
    public static String oracleDeleteName;
    public static int oracleDeleteCalls;
    public static int oracleDeleteMode;
    public static String oracleOpenName;
    public static boolean oracleOpenCreate;
    public static int oracleOpenCalls;
    public static int oracleOpenMode;
    public static byte[] oracleGetData;
    public static int oracleGetId;
    public static int oracleGetCalls;
    public static int oracleGetMode;
    public static int oracleCloseCalls;
    public static int oracleCloseMode;
    public static byte[] oracleSetData;
    public static int oracleSetOffset;
    public static int oracleSetLength;
    public static int oracleSetCalls;

    private RecordStore() {}

    public static RecordStore openRecordStore(String name, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        oracleOpenCalls++;
        oracleOpenName = name;
        oracleOpenCreate = createIfNecessary;
        if (oracleOpenMode == 1) {
            throw new RecordStoreNotFoundException("injected missing store");
        }
        if (oracleOpenMode == 2) {
            throw new RecordStoreException("injected open failure");
        }
        if (oracleOpenMode == 3) {
            throw new AssertionError("injected open error");
        }
        if (oracleOpenMode == 4) {
            throw new NullPointerException("injected open exception");
        }
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

    public static void oracleResetWrite(int openMode) {
        oracleOpenName = null;
        oracleOpenCreate = false;
        oracleOpenCalls = 0;
        oracleOpenMode = openMode;
        oracleGetData = null;
        oracleGetId = 0;
        oracleGetCalls = 0;
        oracleGetMode = 0;
        oracleCloseCalls = 0;
        oracleCloseMode = 0;
        oracleSetData = null;
        oracleSetOffset = 0;
        oracleSetLength = 0;
        oracleSetCalls = 0;
    }

    public static void oracleResetRead(
            int openMode, int getMode, int closeMode, byte[] getData) {
        oracleOpenName = null;
        oracleOpenCreate = false;
        oracleOpenCalls = 0;
        oracleOpenMode = openMode;
        oracleGetData = getData;
        oracleGetId = 0;
        oracleGetCalls = 0;
        oracleGetMode = getMode;
        oracleCloseCalls = 0;
        oracleCloseMode = closeMode;
    }

    public void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {
        oracleCloseCalls++;
        if (oracleCloseMode == 1) {
            throw new RecordStoreNotFoundException("injected missing store");
        }
        if (oracleCloseMode == 2) {
            throw new RecordStoreException("injected close failure");
        }
        if (oracleCloseMode == 3) {
            throw new AssertionError("injected close error");
        }
        if (oracleCloseMode == 4) {
            throw new NullPointerException("injected close exception");
        }
    }

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
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        oracleSetCalls++;
        oracleSetData = newData;
        oracleSetOffset = offset;
        oracleSetLength = numBytes;
    }

    public void deleteRecord(int recordId)
            throws RecordStoreNotOpenException, RecordStoreException {}

    public byte[] getRecord(int recordId)
            throws RecordStoreNotOpenException, RecordStoreException {
        oracleGetCalls++;
        oracleGetId = recordId;
        if (oracleGetMode == 1) {
            throw new RecordStoreNotFoundException("injected missing store");
        }
        if (oracleGetMode == 2) {
            throw new RecordStoreException("injected getRecord failure");
        }
        if (oracleGetMode == 3) {
            throw new AssertionError("injected getRecord error");
        }
        if (oracleGetMode == 4) {
            throw new NullPointerException("injected getRecord exception");
        }
        return oracleGetData;
    }
}
