SELECT INFO_YR /* 정보년도 */,
    LAG_CAT_SNO /* 대분류순번 */,
    MID_CAT_SNO /* 중분류순번 */,
    '['||LAG_CAT_SNO||'-'||MID_CAT_SNO||']'||MID_CAT_TIT_NM AS LAG_CAT_SNO /* 중분류제목명 */,
    MID_CAT_TIT_NM,
    MID_CAT_TXT /* 중분류내용 */,
    CHR_DPT_CD /* 담당부서코드 */,
    F_DPT_INFO(CHR_DPT_CD,'1') AS TKCG_DEPT_NM /* 담당부서명 */,
    REG_YMD /* 등록일자 */,
    RGTR_ID /* 등록자아이디 */,
    MDFCN_YMD /* 수정일자 */,
    MDFCN_PIC_ID /* 수정담당자아이디 */,
    DEL_YN /* 삭제여부 */
FROM TBBADCMR002M WHERE 1=1 AND INFO_YR = #INFO_YR# AND LAG_CAT_SNO =#strLagCatSno# MID_CAT_SNO = #strMidCatSno#