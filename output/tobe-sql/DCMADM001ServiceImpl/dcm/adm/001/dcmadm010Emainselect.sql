SELECT A.DMND_GROUP_ID /* 요청그룹아이디 */,
    A.DMND_GROUP_NM /* 요청그룹명 */,
    A.OWNR_ENO /* 소유자직원전산번호 */,
    A.GROUP_TPCD /* 그룹유형코드 */,
    A.FRST_USER_ID /* 최초사용자아이디 */,
    A.LAST_USER_ID /* 최종사용자아이디 */,
    A.LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    A.LAST_MENU_ID /* 최종메뉴아이디 */,
    A.FRST_REG_DT /* 최초등록일시 */,
    A.LAST_MDFCN_DT /* 최종수정일시 */,
    B.USER_NM /* 사용자명 */,
    C.PROF_RBPRSN_INST_NM /* 증명책임자기관명 */
FROM TB_DCMIDM002M A , TBDCMACM001M B , (SELECT DMND_GROUP_ID /* 요청그룹아이디 */,
    MAX(PROF_RBPRSN_INST_NM) || CASE WHEN COUNT(*) > 0 THEN ' 포함(' || COUNT(*) || ')' ELSE '' END AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */
FROM TBDCMIDM002D A, TBDCMACM015M B WHERE A.DMND_INST_CD = B.INST_CD GROUP BY DMND_GROUP_ID) C WHERE A.OWNR_ENO = B.TRGT_NOP_ENO(+) AND A.DMND_GROUP_ID = C.DMND_GROUP_ID(+) A.GROUP_TPCD = #GROUP_TPCD# A.DMND_GROUP_NM LIKE '%' || #strGroupNm# || '%' ORDER BY A.FRST_REG_DT DESC