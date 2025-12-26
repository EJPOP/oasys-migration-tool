DECLARE BEGIN INSERT INTO TBAEPGKM011M (EVL_YR /* 평가년도 */, EVL_DVSN_CD /* 평가구분코드 */, EVL_STR_DT /* 평가시작일 */, EVL_END_DT /* 평가종료일 */, EVL_ACTV_STR_DT /* 평가활동시작일 */, EVL_ACTV_END_DT /* 평가활동종료일 */, CFMTN_YN /* 확정여부 */, FRST_USER_ID /* 최초사용자ID */, LAST_USER_ID /* 최종사용자ID */, LAST_PRCS_DEPT_CD /* 최종거래부서코드 */, LAST_MENU_ID /* 최종메뉴ID */, FRST_REG_DT /* 최초등록일시 */, LAST_MDFCN_DT /* 최종수정일시 */) VALUES ( #EVL_YR# ,#EVL_DVSN_CD# ,TO_CHAR(TO_DATE(#EVL_STR_DT#), 'YYYYMMDD') ,TO_CHAR(TO_DATE(#EVL_END_DT#), 'YYYYMMDD') ,TO_CHAR(TO_DATE(#EVL_ACTV_STR_DT#), 'YYYYMMDD') ,TO_CHAR(TO_DATE(#EVL_ACTV_END_DT#), 'YYYYMMDD') ,'N' ,#FRT_USR_ID# ,#FNL_USR_ID# ,#FNL_DEAL_DPT_CD# ,#FNL_MNU_ID# ,SYSDATE ,SYSDATE ); INSERT INTO TBAEPGKM013L (EVL_YR /* 평가년도 */, EVL_DVSN_CD /* 평가구분코드 */, KLD_CHR_CAT_CD /* 지식담당분류코드 */, TRGT_NOP_ENO /* 전산번호 */, FRST_USER_ID /* 최초사용자ID */, LAST_USER_ID /* 최종사용자ID */, LAST_PRCS_DEPT_CD /* 최종거래부서코드 */, LAST_MENU_ID /* 최종메뉴ID */, FRST_REG_DT /* 최초등록일시 */, LAST_MDFCN_DT /* 최종수정일시 */) SELECT #EVL_YR# AS #EVL_YR#,
    #EVL_DVSN_CD# AS #EVL_DVSN_CD#,
    KLD_CHR_CAT_CD,
    TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    #FRT_USR_ID# AS FRST_USER_ID /* 최초사용자아이디 */,
    #FNL_USR_ID# AS LAST_USER_ID /* 최종사용자아이디 */,
    #FNL_DEAL_DPT_CD# AS LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    #FNL_MNU_ID# AS LAST_MENU_ID /* 최종메뉴아이디 */,
    SYSDATE AS FRST_REG_DT,
    SYSDATE AS LAST_MDFCN_DT
FROM TBAEPGKM005L WHERE USE_YN = 'Y' AND KLD_CHR_CAT_CD = '04' ; END;