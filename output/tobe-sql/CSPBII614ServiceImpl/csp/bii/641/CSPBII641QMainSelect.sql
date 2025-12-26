SELECT UNQ_CD,
    /*특이사항 코드*/ UNQ_TXT,
    /*특이사항 명*/ UNQ_DT,
    /*특이사항 일*/ STR_WRDO_SC,
    /*비위점수 FROM */ END_WRDO_SC /* 비위점수 TO */
FROM TBCSPBII631C WHERE 1 = 1 AND UNQ_TXT LIKE '%'||#title#||'%' ORDER BY UNQ_CD DESC