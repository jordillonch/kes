package org.jordillonch.kes.cqrs.unit_of_work.infrastructure

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.unit_of_work.domain.EffectQueue
import java.util.concurrent.LinkedBlockingQueue

class SimpleEffectQueue : LinkedBlockingQueue<Effect>(), EffectQueue
