INSERT INTO TBAEPGKM013L (EVL_YR /* 평가년도 */, EVL_DVSN_CD /* 평가구분코드 */, KLD_CHR_CAT_CD /* 지식담당분류코드 */, TRGT_NOP_ENO /* 전산번호 */, FRST_USER_ID /* 최초사용자ID */, LAST_USER_ID /* 최종사용자ID */, LAST_PRCS_DEPT_CD /* 최종거래부서코드 */, LAST_MENU_ID /* 최종메뉴ID */, FRST_REG_DT /* 최초등록일시 */, LAST_MDFCN_DT /* 최종수정일시 */) SELECT #strEvlYr#,
    #strEvlDvsnCd#,
    KLD_CHR_CAT_CD,
    TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    #FRT_USR_ID# AS FRST_USER_ID /* 최초사용자아이디 */,
    #FRT_USR_ID# AS FRST_USER_ID /* 최초사용자아이디 */,
    #FNL_DEAL_DPT_CD# AS LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    #FNL_MNU_ID# AS LAST_MENU_ID /* 최종메뉴아이디 */,
    SYSDATE,
    SYSDATE
FROM TBAEPGKM005L WHERE USE_YN = 'Y' AND KLD_CHR_CAT_CD = '04'