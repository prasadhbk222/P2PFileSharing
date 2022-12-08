public class Constants {
    public static final String COMMON_CONFIG_FILE_NAME = "Common.cfg";
    
    public static final String PEER_INFO_CONFIG_FILE_NAME = "PeerInfo.cfg";
    public static final String WORKING_DIR = System.getProperty("user.dir");
    
    public static final Integer HM_LENGTH = 32;
    public static final String HM_HEADER = "P2PFILESHARINGPROJ";
    public static final Integer HM_HEADER_START = 0;
    public static final Integer HM_HEADER_FIELD = 18;
    public static final Integer HM_ZERO_BITS_START = 18;
    public static final Integer HM_ZERO_BITS_FIELD = 10;
    public static final Integer HM_PEER_ID_START = 28;
    public static final Integer HM_PEER_ID_FIELD = 4;

    public static final Integer AM_MESSAGE_LENGTH_START = 0;
    public static final Integer AM_MESSAGE_LENGTH_FIELD = 4;
    public static final Integer AM_MESSAGE_TYPE_START = 4;
    public static final Integer AM_MESSAGE_TYPE_FIELD = 1;

    public static final Integer PM_PIECE_INDEX_START = 0;
    public static final Integer PM_PIECE_INDEX_FIELD = 4;

    public static enum MessageType {
		CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3),
        HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7), COMPLETED(8);

		private final int value;

		private MessageType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

        public static MessageType getByValue(int value) {
            for (MessageType messageType: MessageType.values()) {
                if (messageType.getValue() == value) {
                    return messageType;
                }
            }
            return null;
        }
    }
}
