SELECT A.STT_INST_NM /* 법령기관명 */,
    /*법령기관명*/ A.STT_LNKG_ID AS STT_LNKG_ID /* 법령연결아이디 */,
    /*법령연결ID*/ A.STT_NM AS STT_NM /* 법령명 */,
    /*법령명*/ A.PRMG_YMD AS PRMG_YMD /* 공포일자 */,
    /*공포일*/ A.STT_SE_NM AS STT_SE_NM /* 법령구분명 */
FROM TB_BADCMR013M A /*PC장비코드*/ WHERE 1=1 AND A.STT_INST_NM LIKE '%'||#strSEARCH_NM#||'%' AND A.STT_NM LIKE '%'||#strSEARCH_NM#||'%' AND A.STT_SE_NM LIKE '%'||#strSEARCH_NM#||'%' ORDER BY A.STT_LNKG_ID