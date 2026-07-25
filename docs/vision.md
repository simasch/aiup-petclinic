# Vision: AIUP PetClinic — Veterinary Clinic Management

> _Reconstructed after the fact from the existing AIUP artifacts (`use_cases.puml`,
> `use_cases/UC-*.md`, `entity_model.md`) and the implementation. It records the
> intent those artifacts express; it did not precede them._

## Overview

AIUP PetClinic supports the front desk of a small veterinary clinic. The desk
needs to know, at any moment, who the clinic's customers are, which animals
belong to them, and what has been done for each animal. Today that knowledge
lives in a mixture of paper and memory, which makes routine work — taking a
phone call, greeting a walk-in, calling an owner back — slower and less
reliable than it should be.

The application gives the desk a single place to keep that information and to
record what happens during a visit. It also puts the clinic's veterinarians and
their specialties in front of the public, so people can see who treats what
without having to ask.

The system is a specification-first re-implementation of the classic
[Spring PetClinic](https://github.com/spring-projects/spring-petclinic) sample
and serves as the running example for a talk on Spec-Driven Development with
the [AI Unified Process](https://unifiedprocess.ai/). Staying faithful to that
sample matters more than inventing new features: the point of the exercise is
that every screen comes from a written use case rather than from a prompt.

## Users and Roles

- **Visitor** — anyone who reaches the application, including prospective
  customers. Visitors orient themselves on the welcome page and look up the
  clinic's veterinarians and their specialties. They have no business with
  customer data.
- **Clinic User** — staff at the front desk. They do everything a visitor can,
  and in addition maintain the clinic's customers: registering owners, finding
  them, keeping their contact details current, recording the animals they own,
  and logging visits.

Pet owners are the clinic's customers but not users of the system. They phone
or walk in, and a clinic user maintains their record on their behalf.

## Goals and Desired Outcomes

- **A caller is identified in seconds.** Staff can find an owner's record from
  the fragment of a name a caller offers on the phone, without knowing how it
  is spelled in full.
- **One screen answers the common question.** Who is this owner, which animals
  do they have, and what has each animal been seen for. Everything staff might
  do next follows from that screen, so nobody has to navigate away mid-call.
- **The clinic can always reach an owner.** A customer record is of no use if
  the phone number is missing or unusable, so the desk is never able to leave
  contact information incomplete.
- **Every visit is attributed to the right animal.** Two animals of the same
  owner must be tellable apart, and the reason for every visit is written down,
  so an animal's history can be relied on later.
- **Staff always know whether their work was saved.** A change either takes
  effect with a visible confirmation, or it is refused with a clear statement of
  what needs fixing.
- **Nobody at the desk is stranded.** When something goes wrong, staff get a
  plain explanation and a way to carry on working, not a technical failure they
  cannot interpret.
- **The public can self-serve on the vet directory.** Anyone can find out which
  vet holds which specialty without occupying a member of staff.

## Business Capabilities

- **Customer register.** Keep a record of every pet owner the clinic deals
  with, including how to contact them, and keep it current as people move or
  change number.
- **Customer lookup.** Retrieve an owner's record from the little that is known
  at the start of a conversation, or browse the customer base as a whole.
- **Animal register.** Record which animals belong to which owner, along with
  the species and age needed to treat them.
- **Visit history.** Record that an animal came in and why, and read back the
  full history of an animal at any time.
- **Veterinarian directory.** Publish the clinic's veterinarians and the
  specialties they hold.

Veterinarians, their specialties, and the list of species the clinic treats
change rarely and are managed by the practice as reference data rather than
through the application.

## Quality Expectations

- **Speed at the desk is a business requirement, not a preference.** Staff are
  usually on the phone with a customer while they work, so lookups and screens
  must keep up with a conversation.
- **The system must remain comfortable as the clinic grows.** A longer customer
  list must not make daily work slower or more fiddly.
- **Mistakes must be cheap to fix.** When the system refuses input, it says
  exactly what is wrong and lets staff correct that one thing.
- **Information is presented in a predictable order,** so staff can scan for
  what they need instead of reading everything.
- **Failures are contained.** An error affects the task in hand, never the
  ability to keep working, and never exposes internals to whoever is looking at
  the screen.
- **The specification is auditable.** Anyone should be able to check that the
  system does what the use cases say it does, rather than take it on trust.

## Business Constraints

- The clinic's existing way of working is the benchmark: terminology, screens,
  and rules follow the established Spring PetClinic sample, and departures need
  a reason.
- The application is used by staff on the clinic's own premises and network.
  There are no customer logins and no remote access for owners.
- The written specification governs. If the software and the specification
  disagree, the software is wrong.

## Out of Scope

- Self-service access for pet owners.
- Accounts, logins, and enforced permissions per member of staff.
- Appointment scheduling in the calendar sense — time slots, assigning a vet to
  a visit, availability, reminders, cancellations.
- Clinical records beyond a note of why an animal came in: no diagnoses,
  treatments, prescriptions, or weight history.
- Removing owners, animals, or visits from the record.
- Billing, invoicing, and payments.
- Reporting and analytics.

---

Technology choices, measurable thresholds, and field-level rules are not part
of this document. They live in [`requirements.md`](requirements.md) as
non-functional requirements and constraints.
