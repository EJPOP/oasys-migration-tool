SELECT D1.INST_CD /* 기관코드 */,
    D2.PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    D1.LINK_YR /* 연계년도 */,
    D1.CPADT_SQNO /* 위탁대행감사순번 */,
    D1.TRGT_INST_CD /* 대상기관코드 */,
    D3.PROF_RBPRSN_INST_NM AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    D1.TRGT_INST_CD /* 대상기관코드 */,
    D1.FRST_USER_ID /* 최초사용자아이디 */,
    D1.LAST_USER_ID /* 최종사용자아이디 */,
    D1.LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    D1.LAST_MENU_ID /* 최종메뉴아이디 */,
    D1.FRST_REG_DT /* 최초등록일시 */,
    D1.LAST_MDFCN_DT /* 최종수정일시 */
FROM TBBADBAP001L D1 /* 감사대상기관내역 */ ,TBDCMACM015M D2 /* 기관정보기본 */ ,TBDCMACM015M D3 /* 기관정보기본 */ WHERE 1=1 AND D1.INST_CD = D2.INST_CD(+) AND D1.TRGT_INST_CD = D3.INST_CD(+) AND D1.INST_CD = #INST_CD# /* 기관코드 */ AND D1.LINK_YR = #LINK_YR# /* 연도 */ AND D1.CPADT_SQNO = #CPADT_SQNO# /* 위임위탁번호 */ ORDER BY D3.PROF_RBPRSN_INST_NM