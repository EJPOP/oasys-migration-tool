SELECT INFO_YR /* 정보년도 */,
    LAG_CAT_SNO /* 대분류순번 */,
    '['||LAG_CAT_SNO||']'||LAG_CAT_TIT_NM AS LAG_CAT_SNO /* 대분류제목명 */,
    LAG_CAT_TXT /* 대분류내용 */,
    REG_YMD /* 등록일자 */,
    RGTR_ID /* 등록자아이디 */,
    MDFCN_YMD /* 수정일자 */,
    MDFCN_PIC_ID /* 수정담당자아이디 */,
    DEL_YN /* 삭제여부 */
FROM TBBADCMR001M WHERE 1=1 AND INFO_YR = #INFO_YR#