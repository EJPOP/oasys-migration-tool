SELECT SLCT_CRTR_DT /* 청탁기준일시 */,
    AM_SET_SNO,
    EXTV_DVSN_CD,
    BSC_M,
    BOKG_M,
    UN_BOKG_M,
    USE_YN /* 사용여부 */,
    FRST_USER_ID /* 최초사용자아이디 */,
    LAST_USER_ID /* 최종사용자아이디 */,
    LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    LAST_MENU_ID /* 최종메뉴아이디 */,
    FRST_REG_DT /* 최초등록일시 */,
    LAST_MDFCN_DT /* 최종수정일시 */
FROM TBCSPMWF004M WHERE 1=1 AND SLCT_CRTR_DT AND SLCT_CRTR_DT &gt;= #fromYmd# AND USE_YN = #USE_YN# ORDER BY SLCT_CRTR_DT DESC, EXTV_DVSN_CD ASC