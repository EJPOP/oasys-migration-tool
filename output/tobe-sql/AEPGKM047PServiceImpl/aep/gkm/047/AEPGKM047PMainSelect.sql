SELECT A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    /*일련번호*/ A.LAG_CAT_CD,
    /*대분류코드*/ A.MID_CAT_CD,
    /*중분류코드*/ A.SALL_CAT_CD,
    /*소분류코드*/ A.RSCH_ASMT_TTL AS RSCH_ASMT_TTL /* 연구과제제목 */,
    /*제목명*/ A.ORG_TYP_CD,
    /*기관유형코드*/ A.MAIN_ORG_NM,
    /*주요기관명*/ B.DTL_TXT
FROM TBAEPGKM019M A /*PC장비코드*/ ,TBAEPGKM020D B WHERE A.FRS_MDM_SQNO = B.FRS_MDM_SQNO AND A.FRS_MDM_SQNO = #FRS_MDM_SQNO#