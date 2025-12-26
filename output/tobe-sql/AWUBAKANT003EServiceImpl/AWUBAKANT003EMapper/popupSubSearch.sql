/* AWU.BAK.BRR.AWUBAKANT003EMapper.popupSubSearch */ SELECT CVLCPT_DOC_ID /* 민원문서아이디 */,
    VT_SBJ_NM,
    FRST_USER_ID /* 최초사용자아이디 */,
    LAST_USER_ID /* 최종사용자아이디 */,
    LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    LAST_MENU_ID /* 최종메뉴아이디 */,
    TO_CHAR(FRST_REG_DT, 'YYYY-MM-DD') AS FRST_REG_DT /* 최초등록일시 */,
    LAST_MDFCN_DT /* 최종수정일시 */
FROM TBAWUBAKANT002M WHERE 1=1 AND CFR_DVSN_CD = #{cfrDvsnCd} AND CFR_SRNO = #{cfrSrno} AND VT_SRNO = #{vtSrno} AND DOC_ID IS NOT NULL