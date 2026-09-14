package com.example.data

enum class IndustryType(val displayName: String) {
    HEALTHCARE("Healthcare & Pharmaceuticals"),
    FINANCE("Financial Services & Banking"),
    LOGISTICS("Logistics & Supply Chain"),
    MANUFACTURING("Manufacturing & Production"),
    RETAIL("Retail & E-Commerce"),
    PROFESSIONAL_SERVICES("Professional Services & Advisory"),
    CUSTOM("Custom / Cross-Industry")
}

data class RecommendedAgentSpec(
    val agentName: String,
    val agentDescription: String,
    val agentType: AgentType,
    val riskClassification: AgentRiskLevel,
    val capabilityProfile: String,
    val modelTier: String,
    val department: String,
    val permissionLevel: AccessLevel = AccessLevel.REGULAR_STAFF
)

data class IndustryTemplate(
    val industryType: IndustryType,
    val displayName: String,
    val description: String,
    val recommendedAgents: List<RecommendedAgentSpec>,
    val defaultWorkflows: List<String>,
    val defaultAiTone: String,
    val defaultAutoApprovalRiskThreshold: Int,
    val defaultAutoApprovalDollarLimit: Double,
    val suggestedAutomationOpportunities: List<String>
) {
    companion object {
        fun getTemplate(type: IndustryType): IndustryTemplate {
            return when (type) {
                IndustryType.HEALTHCARE -> IndustryTemplate(
                    industryType = IndustryType.HEALTHCARE,
                    displayName = "Healthcare & Life Sciences",
                    description = "Tailored for clinical workflows, HIPAA compliance audits, and patient scheduling.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "HIPAA Compliance Agent",
                            agentDescription = "Audits patient record queries and enforces HIPAA privacy guidelines.",
                            agentType = AgentType.COMPLIANCE_AGENT,
                            riskClassification = AgentRiskLevel.CRITICAL,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Regulatory & Privacy",
                            permissionLevel = AccessLevel.READ_ONLY
                        ),
                        RecommendedAgentSpec(
                            agentName = "Patient Scheduling Copilot",
                            agentDescription = "Automates appointment booking, triage routing, and appointment follow-ups.",
                            agentType = AgentType.OPERATIONS_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,SEND_COMMUNICATION",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Patient Services"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Clinical Analytics Engine",
                            agentDescription = "Synthesizes diagnostic throughput and operational hospital metrics.",
                            agentType = AgentType.ANALYTICS_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Medical Operations"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Patient Intake Verification & Triage",
                        "HIPAA Audit Record Logging",
                        "Medical Claims Reconciliation"
                    ),
                    defaultAiTone = "Clinical, Precise & Strictly Compliant",
                    defaultAutoApprovalRiskThreshold = 10,
                    defaultAutoApprovalDollarLimit = 1000.0,
                    suggestedAutomationOpportunities = listOf(
                        "Automated patient intake verification",
                        "Prior authorization document parsing",
                        "Compliance audit trail verification"
                    )
                )

                IndustryType.FINANCE -> IndustryTemplate(
                    industryType = IndustryType.FINANCE,
                    displayName = "Financial Services & Banking",
                    description = "Configured for fraud inspection, automated transaction sign-offs, and treasury reporting.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Finance & Treasury Agent",
                            agentDescription = "Evaluates transaction requests, wire transfers, and liquidity alerts.",
                            agentType = AgentType.FINANCE_AGENT,
                            riskClassification = AgentRiskLevel.HIGH,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,ACCESS_FINANCIAL_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Treasury & Finance",
                            permissionLevel = AccessLevel.WORKFLOW_ADMIN
                        ),
                        RecommendedAgentSpec(
                            agentName = "Financial Reporting Copilot",
                            agentDescription = "Generates quarterly P&L summaries and automated compliance balance sheets.",
                            agentType = AgentType.ANALYTICS_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Accounting"
                        ),
                        RecommendedAgentSpec(
                            agentName = "SEC & AML Compliance Agent",
                            agentDescription = "Screens high-value transactions against Anti-Money Laundering protocols.",
                            agentType = AgentType.COMPLIANCE_AGENT,
                            riskClassification = AgentRiskLevel.CRITICAL,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Risk & Compliance",
                            permissionLevel = AccessLevel.READ_ONLY
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Wire Transfer Risk Evaluation",
                        "Automated Expense Reconciliation",
                        "Anti-Money Laundering Screening"
                    ),
                    defaultAiTone = "Financial Executive & Risk-Averse",
                    defaultAutoApprovalRiskThreshold = 15,
                    defaultAutoApprovalDollarLimit = 2500.0,
                    suggestedAutomationOpportunities = listOf(
                        "Automated ledger reconciliation",
                        "Vendor invoice verification",
                        "Risk assessment on wire requests"
                    )
                )

                IndustryType.LOGISTICS -> IndustryTemplate(
                    industryType = IndustryType.LOGISTICS,
                    displayName = "Logistics & Supply Chain",
                    description = "Designed for fleet routing, inventory tracking, and carrier dispatch automation.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Fleet Dispatch Copilot",
                            agentDescription = "Optimizes route allocation, freight carrier assignments, and transit alerts.",
                            agentType = AgentType.OPERATIONS_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,SEND_COMMUNICATION",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Logistics & Fleet"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Supply Chain Analyst",
                            agentDescription = "Forecasts inventory stockouts and identifies vendor delivery delays.",
                            agentType = AgentType.ANALYTICS_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Supply Chain"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Customs & Safety Auditor",
                            agentDescription = "Verifies international freight manifests and safety compliance records.",
                            agentType = AgentType.COMPLIANCE_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Compliance & Safety"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Carrier Bill of Lading Verification",
                        "Route Exception Alert Dispatch",
                        "Warehouse Reorder Dispatch"
                    ),
                    defaultAiTone = "Operations Briefing & Rapid Execution",
                    defaultAutoApprovalRiskThreshold = 25,
                    defaultAutoApprovalDollarLimit = 5000.0,
                    suggestedAutomationOpportunities = listOf(
                        "Real-time route exception handling",
                        "Supplier SLA tracking",
                        "Manifest data extraction"
                    )
                )

                IndustryType.MANUFACTURING -> IndustryTemplate(
                    industryType = IndustryType.MANUFACTURING,
                    displayName = "Manufacturing & Industrial Operations",
                    description = "Optimizes plant machinery schedules, quality assurance loops, and procurement.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Production Controller Agent",
                            agentDescription = "Monitors assembly throughput, shift schedules, and downtime alerts.",
                            agentType = AgentType.OPERATIONS_AGENT,
                            riskClassification = AgentRiskLevel.HIGH,
                            capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,ANALYZE_DATA",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Plant Operations"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Raw Materials Inventory Agent",
                            agentDescription = "Tracks component stock levels and triggers reorder requests.",
                            agentType = AgentType.FINANCE_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,GENERATE_REPORTS,ACCESS_FINANCIAL_DATA",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Warehouse"
                        ),
                        RecommendedAgentSpec(
                            agentName = "ISO Quality Inspector",
                            agentDescription = "Enforces ISO 9001 standards across assembly line defect reports.",
                            agentType = AgentType.COMPLIANCE_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Quality Control"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Equipment Maintenance Trigger Pipeline",
                        "Assembly Defect Rate Inspection",
                        "Raw Component Reorder Request"
                    ),
                    defaultAiTone = "Technical, Factory-Precision & Direct",
                    defaultAutoApprovalRiskThreshold = 20,
                    defaultAutoApprovalDollarLimit = 3500.0,
                    suggestedAutomationOpportunities = listOf(
                        "Predictive downtime alerts",
                        "Supplier component inspection",
                        "Automated shift handoff reports"
                    )
                )

                IndustryType.RETAIL -> IndustryTemplate(
                    industryType = IndustryType.RETAIL,
                    displayName = "Retail & E-Commerce",
                    description = "Tailored for multi-channel inventory management, customer support, and sales analytics.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Merchandise Inventory Agent",
                            agentDescription = "Monitors product stock across distribution centers and store locations.",
                            agentType = AgentType.OPERATIONS_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ACCESS_FINANCIAL_DATA,EXECUTE_WORKFLOW",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Merchandising"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Customer Escalation Agent",
                            agentDescription = "Resolves refund inquiries, order tracking, and VIP client requests.",
                            agentType = AgentType.EXECUTIVE_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,SEND_COMMUNICATION,EXECUTE_WORKFLOW",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Client Support"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Revenue & Campaign Analyst",
                            agentDescription = "Analyzes promotion performance and customer acquisition costs.",
                            agentType = AgentType.ANALYTICS_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Growth & Marketing"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Automated Refund Verification",
                        "Low-Stock Inventory Reorder Alert",
                        "Customer Feedback Sentiment Classification"
                    ),
                    defaultAiTone = "Customer-Centric & Service-Oriented",
                    defaultAutoApprovalRiskThreshold = 30,
                    defaultAutoApprovalDollarLimit = 1500.0,
                    suggestedAutomationOpportunities = listOf(
                        "Refund and return automation",
                        "Dynamic pricing adjustments",
                        "Inventory restock alerts"
                    )
                )

                IndustryType.PROFESSIONAL_SERVICES -> IndustryTemplate(
                    industryType = IndustryType.PROFESSIONAL_SERVICES,
                    displayName = "Professional Services & Advisory",
                    description = "Configured for client billing, legal/contract analysis, and project onboarding.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Executive Advisory Copilot",
                            agentDescription = "Assists partners with client proposal drafts and strategic briefs.",
                            agentType = AgentType.EXECUTIVE_AGENT,
                            riskClassification = AgentRiskLevel.HIGH,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Executive Office",
                            permissionLevel = AccessLevel.WORKFLOW_ADMIN
                        ),
                        RecommendedAgentSpec(
                            agentName = "Client Billing & Retainer Agent",
                            agentDescription = "Generates client invoices, tracks billable hours, and flags past-due accounts.",
                            agentType = AgentType.FINANCE_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ACCESS_FINANCIAL_DATA,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Client Finance"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Contract & SLA Review Agent",
                            agentDescription = "Parses master service agreements and flags non-standard indemnity terms.",
                            agentType = AgentType.COMPLIANCE_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Legal & Contracts"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "Client Retainer Invoicing",
                        "MSA Contract Clause Analysis",
                        "Project Onboarding Kickoff Pipeline"
                    ),
                    defaultAiTone = "Executive, Authoritative & Concise",
                    defaultAutoApprovalRiskThreshold = 20,
                    defaultAutoApprovalDollarLimit = 5000.0,
                    suggestedAutomationOpportunities = listOf(
                        "Automated retainer billing",
                        "SLA clause extraction",
                        "Proposal generation"
                    )
                )

                IndustryType.CUSTOM -> IndustryTemplate(
                    industryType = IndustryType.CUSTOM,
                    displayName = "Custom Organizational Setup",
                    description = "Flexible workspace profile adaptable to any industry or specialized operational model.",
                    recommendedAgents = listOf(
                        RecommendedAgentSpec(
                            agentName = "Nova Operations Copilot",
                            agentDescription = "General-purpose agent for workflow automation and task dispatch.",
                            agentType = AgentType.OPERATIONS_AGENT,
                            riskClassification = AgentRiskLevel.LOW,
                            capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,GENERATE_REPORTS",
                            modelTier = "GEMINI_2_5_FLASH",
                            department = "Operations"
                        ),
                        RecommendedAgentSpec(
                            agentName = "Executive Advisory Agent",
                            agentDescription = "Assists leadership with analytics, strategy briefs, and governance.",
                            agentType = AgentType.EXECUTIVE_AGENT,
                            riskClassification = AgentRiskLevel.MEDIUM,
                            capabilityProfile = "READ_DATA,ANALYZE_DATA,REQUEST_APPROVAL",
                            modelTier = "GEMINI_2_5_PRO",
                            department = "Executive Office"
                        )
                    ),
                    defaultWorkflows = listOf(
                        "General Task Dispatch",
                        "Executive Briefing Generation"
                    ),
                    defaultAiTone = "Professional & Data-Driven",
                    defaultAutoApprovalRiskThreshold = 20,
                    defaultAutoApprovalDollarLimit = 2500.0,
                    suggestedAutomationOpportunities = listOf(
                        "General task dispatching",
                        "Executive reporting"
                    )
                )
            }
        }
    }
}
